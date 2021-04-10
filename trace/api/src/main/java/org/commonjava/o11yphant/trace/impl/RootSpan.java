package org.commonjava.o11yphant.trace.impl;

import org.commonjava.o11yphant.trace.RootSpanDecorator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

public class RootSpan
                implements SpanAdapter
{
    private SpanAdapter delegate;

    private RootSpanDecorator rootSpanDecorator;

    public RootSpan( SpanAdapter delegate, RootSpanDecorator rootSpanDecorator )
    {
        this.delegate = delegate;
        this.rootSpanDecorator = rootSpanDecorator;
    }

    @java.lang.Override
    public String getTraceId()
    {
        return delegate.getTraceId();
    }

    @java.lang.Override
    public String getSpanId()
    {
        return delegate.getSpanId();
    }

    @java.lang.Override
    public void addField( String name, Object value )
    {
        delegate.addField( name, value );
    }

    @java.lang.Override
    public void close()
    {
        rootSpanDecorator.decorate( delegate );
        delegate.close();
    }

    @java.lang.Override
    public void setInProgressField( String key, Object value )
    {
        delegate.setInProgressField( key, value );
    }

    @java.lang.Override
    public <V> V getInProgressField( String key, V defValue )
    {
        return delegate.getInProgressField( key, defValue );
    }

    @java.lang.Override
    public void clearInProgressField( String key )
    {
        delegate.clearInProgressField( key );
    }
}
