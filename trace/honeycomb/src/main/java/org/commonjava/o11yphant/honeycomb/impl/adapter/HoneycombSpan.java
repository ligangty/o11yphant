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
package org.commonjava.o11yphant.honeycomb.impl.adapter;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HoneycombSpan implements SpanAdapter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Span span;

    private boolean localRoot;

    private Beeline beeline;

    private Map<String, Object> inProgress = new HashMap<>();

    private boolean closed = false;

    private void checkClosed()
    {
        if ( closed )
        {
            String msg = String.format( "Span: %s (%s) in trace: %s is closed!", span.getSpanName(), span.getSpanId(), span.getTraceId() );
            if ( logger.isTraceEnabled() )
            {
                throw new IllegalStateException( msg );
            }
            else
            {
                logger.warn( msg );
            }
        }
    }

    public HoneycombSpan( Span span, boolean localRoot, Beeline beeline )
    {
        this.span = span;
        this.localRoot = localRoot;
        this.beeline = beeline;
    }

    @Override
    public boolean isLocalRoot()
    {
        return localRoot;
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
        logger.trace( "Adding field: {} to: {} / {} (no-op? {})", key, span.getTraceId(), span.getSpanId(), span.isNoop() );
        checkClosed();
        span.addField( key, value );
    }

    @Override
    public Map<String, Object> getFields()
    {
        return span.getFields();
    }

    @Override
    public void close()
    {
        span.addFields( inProgress );
        closed = true;
        logger.trace( "HONEYCOMB SPAN CLOSE: {}", getSpanId() );
        span.close();
        if ( isLocalRoot() )
        {
            logger.trace( "HONEYCOMB TRACE CLOSE: {} (via span: {})", getTraceId(), getSpanId() );
            beeline.getTracer().endTrace();
        }
    }

    @Override
    public void setInProgressField( String key, Object value )
    {
        checkClosed();
        inProgress.put( key, value );
    }

    @Override
    public <V> V getInProgressField( String key, V defValue )
    {
        return (V) inProgress.getOrDefault( key, defValue );
    }

    @Override
    public Map<String, Object> getInProgressFields()
    {
        return inProgress;
    }

    @Override
    public void clearInProgressField( String key )
    {
        inProgress.remove( key );
    }

    public PropagationContext getPropagationContext()
    {
        return new PropagationContext( span.getTraceId(), span.getSpanId(), span.getDataset(), span.getTraceFields() );
    }

    public String toString()
    {
        return "HoneycombSpan(adapter class, trace: " + getTraceId() + ", span: " + getSpanId() + ")";
    }
}
