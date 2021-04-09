package org.commonjava.o11yphant.otel.impl;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpan;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpanContext;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.SpanProvider;

import java.util.Optional;

public class OtelSpanProvider implements SpanProvider<OtelType>
{

    private OpenTelemetry otel;

    private Tracer tracer;

    public OtelSpanProvider( OpenTelemetrySdk otel, Tracer tracer )
    {
        this.otel = otel;
        this.tracer = tracer;
    }

    @Override
    public SpanAdapter<OtelType> startServiceRootSpan( String spanName, Optional<SpanContext<OtelType>> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        if ( parentContext.isPresent() )
        {
            Context ctx = (( OtelSpanContext ) parentContext.get()).getContext();
            spanBuilder.setParent( ctx);
        }

        Span span = spanBuilder.startSpan();
        span.makeCurrent();
        return new OtelSpan( span );
    }

    @Override
    public SpanAdapter<OtelType> startChildSpan( String spanName, Optional<SpanContext<OtelType>> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        if ( parentContext.isPresent() )
        {
            Context ctx = (( OtelSpanContext ) parentContext.get()).getContext();
            spanBuilder.setParent( ctx);
        }
        else
        {
            Context ctx = Context.current();
            if ( ctx != null )
            {
                spanBuilder.setParent( ctx );
            }
        }

        Span span = spanBuilder.startSpan();
        span.makeCurrent();
        return new OtelSpan( span );
    }
}
