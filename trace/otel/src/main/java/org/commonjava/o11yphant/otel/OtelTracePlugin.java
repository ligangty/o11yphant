/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.o11yphant.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.commonjava.o11yphant.otel.impl.OtelContextPropagator;
import org.commonjava.o11yphant.otel.impl.OtelSpanProvider;
import org.commonjava.o11yphant.otel.impl.OtelThreadTracingContext;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OtelTracePlugin
        implements O11yphantTracePlugin<OtelType>
{
    private OtelThreadTracingContext threadTracingContext;

    private OtelContextPropagator contextPropagator;

    private OtelSpanProvider spanProvider;

    public OtelTracePlugin( TracerConfiguration traceConfiguration, OtelConfiguration otelConfig,
                            SpanExporter... exporters )
    {
        if ( traceConfiguration.isEnabled() )
        {

            //FIXME: This needs to be more exposed to configuration options, especially for endpoint and exporter formats.
            if ( exporters == null || exporters.length < 1 )
            {
                List<SpanExporter> exp = new ArrayList<>();
                if ( traceConfiguration.isConsoleTransport() )
                {
                    exp.add( LoggingSpanExporter.create() );
                }

                OtlpGrpcSpanExporterBuilder grpcExporterBuilder = OtlpGrpcSpanExporter.builder();
                grpcExporterBuilder.setEndpoint( otelConfig.getGrpcEndpointUri() );
                Map<String, String> exporterHeaders = otelConfig.getGrpcHeaders();
                if ( exporterHeaders != null )
                {
                    exporterHeaders.forEach( grpcExporterBuilder::addHeader );
                }

                grpcExporterBuilder.build();
                exp.add( grpcExporterBuilder.build() );

                exporters = exp.toArray( new SpanExporter[]{} );
            }

            SpanProcessor processor = BatchSpanProcessor.builder( SpanExporter.composite( exporters ) ).build();

            SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder().addSpanProcessor( processor );

            Map<String, String> otelResources = otelConfig.getResources();
            if ( otelResources != null && !otelResources.isEmpty() )
            {
                Attributes attrs = Resource.getDefault().getAttributes();
                AttributesBuilder builder = Attributes.builder().putAll( attrs );
                otelResources.forEach( builder::put );
                tracerProviderBuilder.setResource( Resource.create( builder.build() ) );
            }

            SdkTracerProvider tracerProvider = tracerProviderBuilder.build();

            OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
                                                    .setTracerProvider( tracerProvider )
                                                    .setPropagators( ContextPropagators.create(
                                                                    W3CTraceContextPropagator.getInstance() ) )
                                                    .buildAndRegisterGlobal();

            Tracer tracer = otel.getTracer( otelConfig.getInstrumentationName(), otelConfig.getInstrumentationVersion() );

            this.contextPropagator = new OtelContextPropagator( otel );
            this.spanProvider = new OtelSpanProvider( otel, tracer );
            this.threadTracingContext = new OtelThreadTracingContext();
        }
    }

    @Override
    public SpanProvider<OtelType> getSpanProvider()
    {
        return spanProvider;
    }

    @Override
    public ContextPropagator<OtelType> getContextPropagator()
    {
        return contextPropagator;
    }

    @Override
    public ThreadTracingContext<OtelType> getThreadTracingContext()
    {
        return threadTracingContext;
    }
}
