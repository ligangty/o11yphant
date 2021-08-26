package org.commonjava.o11yphant.trace.impl;

import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This span adapter decorates the span when it is finally closed. It is designed to count the number of close() calls
 * and match them to the number of decorators present. When the close call count does match the decorator count, the span
 * gets closed down.
 *
 * The purpose is to allow something like a REST request to return when a transfer stream is still pending. In that
 * case, you want to add some final span information when the transfer completes, or else you could skew some measurements
 * compared to what the client actually experiences. If we're streaming a file back to a caller, we don't want the
 * servlet to terminate the span...we want the transfer thread to do it.
 *
 * @see org.commonjava.o11yphant.trace.TraceManager#addCloseBlockingDecorator(Optional, CloseBlockingDecorator)
 * @see CloseBlockingDecorator
 */
public class CloseBlockingSpan implements SpanAdapter
{

    private final SpanAdapter delegate;

    private List<CloseBlockingDecorator> looseInjectors = new ArrayList<>();

    private AtomicInteger looseCloseCalls = new AtomicInteger( 0 );

    public CloseBlockingSpan( SpanAdapter delegate )
    {
        this.delegate = delegate;
    }

    public CloseBlockingSpan( SpanAdapter delegate, CloseBlockingDecorator injector )
    {
        this.delegate = delegate;
        looseInjectors.add( 0, injector );
    }

    public void addInjector( CloseBlockingDecorator injector )
    {
        looseInjectors.add( 0, injector );
    }
    @Override
    public boolean isLocalRoot()
    {
        return delegate.isLocalRoot();
    }

    @Override
    public String getTraceId()
    {
        return delegate.getTraceId();
    }

    @Override
    public String getSpanId()
    {
        return delegate.getSpanId();
    }

    @Override
    public void addField( String name, Object value )
    {
        delegate.addField( name, value );
    }

    @Override
    public Map<String, Object> getFields()
    {
        return delegate.getFields();
    }

    @Override
    public void close()
    {
        if ( !looseInjectors.isEmpty() )
        {
            if ( looseCloseCalls.incrementAndGet() >= looseInjectors.size() )
            {
                looseInjectors.forEach( i->i.decorateSpanAtClose( delegate ) );
            }
            else
            {
                return;
            }
        }

        delegate.close();
    }

    @Override
    public void setInProgressField( String key, Object value )
    {
        delegate.setInProgressField( key, value );
    }

    @Override
    public <V> V getInProgressField( String key, V defValue )
    {
        return delegate.getInProgressField( key, defValue );
    }

    @Override
    public void clearInProgressField( String key )
    {
        delegate.clearInProgressField( key );
    }

    @Override
    public Map<String, Object> getInProgressFields()
    {
        return delegate.getInProgressFields();
    }

}
