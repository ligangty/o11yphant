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

import org.commonjava.o11yphant.trace.SpanFieldsDecorator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.Map;

public class FieldInjectionSpan
                implements SpanAdapter
{
    private SpanAdapter delegate;

    private SpanFieldsDecorator spanFieldsDecorator;

    public FieldInjectionSpan( SpanAdapter delegate, SpanFieldsDecorator spanFieldsDecorator )
    {
        this.delegate = delegate;
        this.spanFieldsDecorator = spanFieldsDecorator;
        spanFieldsDecorator.decorateOnStart( delegate );
    }

    @Override
    public boolean isLocalRoot()
    {
        // We're injecting context gathered during local service execution, so this must be the root span for the local
        // service...
        return true;
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

    @Override
    public Map<String, Object> getFields()
    {
        return delegate.getFields();
    }

    @java.lang.Override
    public void close()
    {
        spanFieldsDecorator.decorateOnClose( delegate );
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

    @Override
    public Map<String, Object> getInProgressFields()
    {
        return delegate.getInProgressFields();
    }
}
