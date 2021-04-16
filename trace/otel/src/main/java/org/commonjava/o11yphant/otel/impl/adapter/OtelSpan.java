package org.commonjava.o11yphant.otel.impl.adapter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.HashMap;
import java.util.Map;

public class OtelSpan
                implements SpanAdapter
{
    private Span span;

    private boolean localRoot;

    private Map<String, Object> inProgress = new HashMap<>();

    private Map<String, Object> attributes = new HashMap<>();

    public OtelSpan( Span span, boolean localRoot )
    {
        this.span = span;
        this.localRoot = localRoot;
    }

    @Override
    public boolean isLocalRoot()
    {
        return localRoot;
    }

    @Override
    public String getTraceId()
    {
        return span.getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId()
    {
        return span.getSpanContext().getSpanId();
    }

    @Override
    public void addField( String name, Object value )
    {
        attributes.put( name, String.valueOf( value ) );
    }

    @Override
    public Map<String, Object> getFields()
    {
        return attributes;
    }

    @Override
    public void close()
    {
        attributes.forEach( ( k, v ) -> span.setAttribute( k, (String) v ) );

        inProgress.forEach( ( k, v ) -> {
            if ( v instanceof Long )
            {
                span.setAttribute( k, (Long) v );
            }
            else if ( v instanceof Double )
            {
                span.setAttribute( k, (Double) v );
            }
            else if ( v instanceof Integer )
            {
                span.setAttribute( k, Long.valueOf( (Integer) v ) );
            }
            else
            {
                span.setAttribute( k, String.valueOf( v ) );
            }
        } );

        span.end();
    }

    @Override
    public void setInProgressField( String key, Object value )
    {
        inProgress.put( key, value );
    }

    @Override
    public <T> T getInProgressField( String key, T defValue )
    {
        return (T) inProgress.getOrDefault( key, defValue );
    }

    @Override
    public void clearInProgressField( String key )
    {
        inProgress.remove( key );
    }

    @Override
    public Map<String, Object> getInProgressFields()
    {
        return inProgress;
    }

    public SpanContext getSpanContext()
    {
        return span.getSpanContext();
    }

    public Scope makeCurrent()
    {
        return span.makeCurrent();
    }
}
