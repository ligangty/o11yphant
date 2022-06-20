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

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.Map;

public abstract class SpanWrapper
                implements SpanAdapter
{
    private SpanAdapter delegate;

    protected SpanWrapper( SpanAdapter delegate )
    {
        this.delegate = delegate;
    }

    public boolean isWrapper()
    {
        return true;
    }

    public SpanAdapter getBaseInstance()
    {
        SpanAdapter span = getDelegate();
        while ( span.isWrapper() )
        {
            span = ((SpanWrapper) span).getDelegate();
        }

        return span;
    }

    public SpanAdapter getDelegate()
    {
        return delegate;
    }

    public boolean isLocalRoot()
    {
        return delegate.isLocalRoot();
    }

    public String getTraceId()
    {
        return delegate.getTraceId();
    }

    public String getSpanId()
    {
        return delegate.getSpanId();
    }

    public void addField( String name, Object value )
    {
        delegate.addField( name, value );
    }

    public Map<String, Object> getFields()
    {
        return delegate.getFields();
    }

    public void close()
    {
        delegate.close();
    }

    public void setInProgressField( String key, Double value )
    {
        delegate.setInProgressField( key, value );
    }

    public Double getInProgressField( String key, Double defValue )
    {
        return delegate.getInProgressField( key, defValue );
    }

    public Double updateInProgressField( String key, Double value )
    {
        return delegate.updateInProgressField( key, value );
    }

    public void clearInProgressField( String key )
    {
        delegate.clearInProgressField( key );
    }

    public Map<String, Double> getInProgressFields()
    {
        return delegate.getInProgressFields();
    }

    public String toString()
    {
        return getClass().getSimpleName() + "(" + getDelegate().toString() + ")";
    }
}
