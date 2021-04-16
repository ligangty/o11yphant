package org.commonjava.o11yphant.otel;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
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
import java.util.Arrays;
import java.util.List;

public class OtelTracePlugin implements O11yphantTracePlugin<OtelType>
{
    private OtelThreadTracingContext threadTracingContext;

    private OtelContextPropagator contextPropagator;

    private OtelSpanProvider spanProvider;

    public OtelTracePlugin( TracerConfiguration traceConfiguration, OtelConfiguration otelConfig, SpanExporter...exporters )
    {
        if ( traceConfiguration.isEnabled() )
        {
            //FIXME: This needs to be more exposed to configuration options, especially for endpoint and exporter formats.
            if ( exporters == null || exporters.length < 1 )
            {
                List<SpanExporter> exp = new ArrayList<>();
                if ( traceConfiguration.isConsoleTransport() )
                {
                    exp.add( new LoggingSpanExporter() );
                }

                exp.add( OtlpGrpcSpanExporter.builder().setEndpoint( otelConfig.getGrpcEndpointUri() ).build() );

                exporters = exp.toArray( new SpanExporter[]{} );
            }

            SpanProcessor processor = BatchSpanProcessor.builder( SpanExporter.composite( exporters ) ).build();

            SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor( processor ).build();

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
