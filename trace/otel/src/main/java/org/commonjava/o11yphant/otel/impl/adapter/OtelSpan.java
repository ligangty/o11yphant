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
package org.commonjava.o11yphant.otel.impl.adapter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OtelSpan
        implements SpanAdapter
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final Span span;

    private final boolean localRoot;

    private final Map<String, Double> inProgress = new HashMap<>();

    private final Map<String, Object> attributes = new HashMap<>();

    public OtelSpan( Span span, boolean localRoot )
    {
        this.span = span;
        this.localRoot = localRoot;
    }

    @Override
    public boolean isLocalRoot()
    {
        return localRoot;
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
        attributes.put( name, String.valueOf( value ) );
    }

    @Override
    public Map<String, Object> getFields()
    {
        return attributes;
    }

    @Override
    public void close()
    {
        attributes.forEach( ( k, v ) -> span.setAttribute( k, (String) v ) );
        inProgress.forEach( span::setAttribute );

        SpanContext context = span.getSpanContext();
        logger.trace( "Closing span {} in trace {}", getSpanId(), getTraceId() );
        logger.trace( "Span in progress attributes {}", inProgress );
        logger.trace( "Span attributes {}", attributes );
        logger.trace( "==============================" );
        span.end();
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
    public Optional<org.commonjava.o11yphant.trace.spi.adapter.SpanContext> getSpanContext()
    {
        return Optional.of( new OtelSpanContext( Context.current().with( span ) ) );
    }

    public Scope makeCurrent()
    {
        return span.makeCurrent();
    }

    @Override
    public String toString()
    {
        return span.toString();
    }
}
