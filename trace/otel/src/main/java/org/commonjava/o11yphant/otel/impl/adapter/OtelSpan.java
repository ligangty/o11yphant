package org.commonjava.o11yphant.otel.impl.adapter;

import io.opentelemetry.api.trace.Span;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.HashMap;
import java.util.Map;

public class OtelSpan
                implements SpanAdapter<OtelType>
{
    private Span span;

    private Map<String, Object> inProgress = new HashMap<>();

    public OtelSpan( Span span )
    {
        this.span = span;
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
        span.setAttribute( name, String.valueOf( value ) );
    }

    @Override
    public void close()
    {
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

}
