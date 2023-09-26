/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
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
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OtelUtil
{
    private final static Logger logger = LoggerFactory.getLogger( OtelUtil.class );

    private static volatile OpenTelemetry defaultOtel;

    private static final Object mutex = new Object();

    private static void set( OpenTelemetry openTelemetry )
    {
        synchronized ( mutex )
        {
            if ( defaultOtel != null )
            {
                logger.warn( "Note: Otel has been set up! Please check somewhere else if you have set it!" );
            }
            defaultOtel = openTelemetry;
        }
    }

    public static OpenTelemetry getOpenTelemetry( TracerConfiguration traceConfiguration, OtelConfiguration otelConfig,
                                                  SpanExporter... exporters )
    {
        logger.debug( "Trace enabled with Otel trace plugin." );
        SpanExporter[] spanExporters = exporters;
        //FIXME: This needs to be more exposed to configuration options, especially for endpoint and exporter formats.
        if ( exporters == null || exporters.length < 1 )
        {
            final String grpcEndpoint = otelConfig.getGrpcEndpointUri();
            logger.info( "Trace grpc endpoint is configured as: {}", grpcEndpoint );
            List<SpanExporter> exp = new ArrayList<>();
            if ( traceConfiguration.isConsoleTransport() )
            {
                exp.add( LoggingSpanExporter.create() );
            }

            OtlpGrpcSpanExporterBuilder grpcExporterBuilder = OtlpGrpcSpanExporter.builder();
            grpcExporterBuilder.setEndpoint( grpcEndpoint );
            Map<String, String> exporterHeaders = otelConfig.getGrpcHeaders();
            if ( exporterHeaders != null )
            {
                exporterHeaders.forEach( grpcExporterBuilder::addHeader );
            }

            grpcExporterBuilder.build();
            exp.add( grpcExporterBuilder.build() );

            spanExporters = exp.toArray( new SpanExporter[] {} );
        }

        SpanProcessor processor = BatchSpanProcessor.builder( SpanExporter.composite( spanExporters ) ).build();

        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder().addSpanProcessor( processor );

        Map<String, String> otelResources = otelConfig.getResources();
        if ( otelResources != null && !otelResources.isEmpty() )
        {
            logger.debug( "Additional Trace Attributes for OTEL: {}", otelResources );
            AttributesBuilder builder = Attributes.builder();
            otelResources.forEach( builder::put );
            Resource resource = Resource.getDefault().merge( Resource.create( builder.build() ) );
            tracerProviderBuilder.setResource( resource );
        }

        SdkTracerProvider tracerProvider = tracerProviderBuilder.build();

        OpenTelemetry otel;
        try
        {
            otel = OpenTelemetrySdk.builder()
                                   .setTracerProvider( tracerProvider )
                                   .setPropagators(
                                           ContextPropagators.create( W3CTraceContextPropagator.getInstance() ) )
                                   .build();
            if ( getDefaultOpenTelemetry() == null )
            {
                set( otel );
            }
            else
            {
                logger.info(
                        "A default opentelemetry has been setup. You can call getDefaultOpenTelemetry() to get it." );
            }
            logger.debug( "The OpenTelemetry instance has been setup successfully." );
        }
        catch ( IllegalStateException e )
        {
            logger.warn( "The OpenTelemetry instance has not been setup successfully. Error: {}", e.getMessage() );
            if ( getDefaultOpenTelemetry() != null )
            {
                logger.warn( "Will use the default OpenTelemetry as it's setup somewhere." );
                return getDefaultOpenTelemetry();
            }
            logger.warn( "Will use the global one." );
            otel = GlobalOpenTelemetry.get();
        }
        return otel;
    }

    public static OpenTelemetry getDefaultOpenTelemetry()
    {
        return defaultOtel;
    }
}
