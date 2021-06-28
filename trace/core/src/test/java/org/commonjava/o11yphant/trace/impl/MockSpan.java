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
package org.commonjava.o11yphant.trace.impl;

import org.commonjava.cdi.util.weft.ContextSensitiveWeakHashMap;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.HashMap;
import java.util.Map;

public class MockSpan
    implements SpanAdapter
{
    private boolean isLocalRoot;

    private String traceId;

    private String spanId;

    private Map<String, Object> fields = new HashMap<>();

    private Map<String, Object> inProgress = new HashMap<>();

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

    public void setInProgress( Map<String, Object> inProgress )
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
    public void setInProgressField( String key, Object value )
    {
        inProgress.put( key, value );
    }

    @Override
    public <V> V getInProgressField( String key, V defValue )
    {
        return (V) inProgress.getOrDefault( key, defValue);
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
}
