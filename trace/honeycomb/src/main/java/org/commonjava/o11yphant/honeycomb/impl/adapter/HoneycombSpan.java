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
import java.util.function.BiConsumer;

public class HoneycombSpan implements SpanAdapter
{
    private final Span span;

    private Beeline beeline;

    private Map<String, Object> inProgress = new HashMap<>();

    public HoneycombSpan( Span span, Beeline beeline )
    {
        this.span = span;
        this.beeline = beeline;
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
    public Map<String, Object> getFields()
    {
        return span.getFields();
    }

    @Override
    public void close()
    {
        span.addFields( inProgress );
        span.close();
        if ( span.getParentSpanId() == null )
        {
            beeline.getTracer().endTrace();
        }
    }

    @Override
    public void setInProgressField( String key, Object value )
    {
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
}
