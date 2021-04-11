package org.commonjava.o11yphant.honeycomb.impl.adapter;

import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.function.BiConsumer;

public class HoneycombSpan implements SpanAdapter
{
    private final Span span;

    private BiConsumer<SpanAdapter, Span> closer;

    public HoneycombSpan( Span span )
    {
        this.span = span;
    }

    public HoneycombSpan( Span span, BiConsumer<SpanAdapter, Span> closer )
    {
        this.span = span;
        this.closer = closer;
    }

    @Override
    public boolean isLocalRoot()
    {
        return span.getParentSpanId() == null;
    }

    @Override
    public String getTraceId()
    {
        return span.getTraceId();
    }

    @Override
    public String getSpanId()
    {
        return span.getSpanId();
    }

    @Override
    public void addField( String key, Object value )
    {
        span.addField( key, value );
    }

    @Override
    public void close()
    {
        if ( closer != null )
        {
            closer.accept( this, span );
        }
        else
        {
            span.close();
        }
    }

    @Override
    public void setInProgressField( String key, Object value )
    {
        span.addField( key, value );
    }

    @Override
    public <V> V getInProgressField( String key, V defValue )
    {
        return (V) span.getFields().getOrDefault( key, defValue );
    }

    @Override
    public void clearInProgressField( String key )
    {
        span.addField( key, null );
    }

    public PropagationContext getPropagationContext()
    {
        return new PropagationContext( span.getTraceId(), span.getSpanId(), span.getDataset(), span.getTraceFields() );
    }
}
