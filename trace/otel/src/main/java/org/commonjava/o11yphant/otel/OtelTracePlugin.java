package org.commonjava.o11yphant.otel;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.otel.impl.OtelContextPropagator;
import org.commonjava.o11yphant.otel.impl.OtelSpanProvider;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

public class OtelTracePlugin implements O11yphantTracePlugin<OtelType>
{
    private OtelContextPropagator contextPropagator;

    private OtelSpanProvider spanProvider;

    public OtelTracePlugin( OtelConfiguration otelConfig )
    {
        //FIXME: This needs to be more exposed to configuration options, especially for endpoint and exporter formats.
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                                            .addSpanProcessor( BatchSpanProcessor.builder(
                                                                            OtlpGrpcSpanExporter.builder().build() ).build() )
                                                            .build();

        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
                                                .setTracerProvider( tracerProvider )
                                                .setPropagators( ContextPropagators.create(
                                                                W3CTraceContextPropagator.getInstance() ) )
                                                .buildAndRegisterGlobal();


        Tracer tracer = otel.getTracer( otelConfig.getInstrumentationName(), otelConfig.getInstrumentationVersion() );

        this.contextPropagator = new OtelContextPropagator( otel );
        this.spanProvider = new OtelSpanProvider( otel, tracer );

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
}
