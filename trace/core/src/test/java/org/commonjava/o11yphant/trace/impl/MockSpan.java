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
package org.commonjava.o11yphant.trace.impl;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MockSpan
        implements SpanAdapter
{
    private boolean isLocalRoot;

    private String traceId;

    private String spanId;

    private Map<String, Object> fields = new HashMap<>();

    private Map<String, Double> inProgress = new HashMap<>();

    public void setLocalRoot( boolean localRoot )
    {
        isLocalRoot = localRoot;
    }

    public void setTraceId( String traceId )
    {
        this.traceId = traceId;
    }

    public void setSpanId( String spanId )
    {
        this.spanId = spanId;
    }

    public void setFields( Map<String, Object> fields )
    {
        this.fields = fields;
    }

    public void setInProgress( Map<String, Double> inProgress )
    {
        this.inProgress = inProgress;
    }

    @Override
    public boolean isLocalRoot()
    {
        return isLocalRoot;
    }

    @Override
    public String getTraceId()
    {
        return traceId;
    }

    @Override
    public String getSpanId()
    {
        return spanId;
    }

    @Override
    public void addField( String name, Object value )
    {
        fields.put( name, value );
    }

    @Override
    public Map<String, Object> getFields()
    {
        return fields;
    }

    @Override
    public void close()
    {
    }

    @Override
    public void setInProgressField( String key, Double value )
    {
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
        Double mappedVal = inProgress.getOrDefault( key, 0.0 );
        mappedVal += value;
        inProgress.put( key, mappedVal );
        return mappedVal;
    }

    @Override
    public void clearInProgressField( String key )
    {
        inProgress.remove( key );
    }

    @Override
    public Map<String, Double> getInProgressFields()
    {
        return inProgress;
    }

    @Override
    public Optional<SpanContext> getSpanContext()
    {
        return Optional.empty();
    }
}
