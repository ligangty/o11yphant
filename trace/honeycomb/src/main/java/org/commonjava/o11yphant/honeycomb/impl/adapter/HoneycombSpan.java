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
package org.commonjava.o11yphant.honeycomb.impl.adapter;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HoneycombSpan implements SpanAdapter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Span span;

    private final boolean localRoot;

    private final Beeline beeline;

    private final Map<String, Double> inProgress = new ConcurrentHashMap<>();

    private boolean closed = false;

    private void checkClosed()
    {
        if ( closed )
        {
            String msg = String.format( "Span: %s (%s) in trace: %s is closed!", span.getSpanName(), span.getSpanId(), span.getTraceId() );
            logger.warn( msg );
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
        synchronized ( span )
        {
            span.addField( key, value );
        }
    }

    @Override
    public Map<String, Object> getFields()
    {
        synchronized ( span )
        {
            return new ConcurrentHashMap<>( span.getFields() );
        }
    }

    @Override
    public synchronized void close()
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
    public synchronized void setInProgressField( String key, Double value )
    {
        checkClosed();
        inProgress.put( key, value );
    }

    @Override
    public Double getInProgressField( String key, Double defValue )
    {
        return inProgress.getOrDefault( key, defValue );
    }

    @Override
    public synchronized Double updateInProgressField( String key, Double value )
    {
        checkClosed();
        Double mappedVal = inProgress.getOrDefault( key, 0.0 );
        mappedVal += value;
        inProgress.put( key, mappedVal );
        return mappedVal;
    }

    @Override
    public Map<String, Double> getInProgressFields()
    {
        return inProgress;
    }

    @Override
    public synchronized void clearInProgressField( String key )
    {
        inProgress.remove( key );
    }

    public PropagationContext getPropagationContext()
    {
        return new PropagationContext( span.getTraceId(), span.getSpanId(), span.getDataset(), span.getTraceFields() );
    }

    public String toString()
    {
        return "HoneycombSpan(" + span.getSpanName() + ", trace: " + getTraceId() + ", span: " + getSpanId() + ")";
    }
}
