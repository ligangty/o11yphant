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
package org.commonjava.o11yphant.otel.impl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpan;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpanContext;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class OtelSpanProvider
        implements SpanProvider
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final Tracer tracer;

    @SuppressWarnings( "PMD" )
    public OtelSpanProvider( Tracer tracer )
    {
        this.tracer = tracer;
    }

    @Override
    public SpanAdapter startServiceRootSpan( String spanName, Optional<SpanContext> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        logger.trace( "Start a new server span {}", spanName );
        if ( parentContext.isPresent() )
        {
            Context ctx = ( (OtelSpanContext) parentContext.get() ).getContext();
            logger.trace( "The span {} is using a parent context {}", spanName, ctx );
            spanBuilder.setParent( ctx );
        }
        else
        {
            spanBuilder.setNoParent();
        }

        return startSpan( spanBuilder, SpanKind.SERVER, true );
    }

    @Override
    public SpanAdapter startChildSpan( String spanName, Optional<SpanContext> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        boolean isRoot = true;
        logger.trace( "Start a new child span {}", spanName );
        if ( parentContext.isPresent() )
        {
            Context ctx = ( (OtelSpanContext) parentContext.get() ).getContext();
            spanBuilder.setParent( ctx );
            logger.trace( "The span {} is using a parent context {}", spanName, ctx );
            isRoot = false;
        }
        else
        {
            Context ctx = Context.current();
            logger.trace( "The span {} is using the current context {} as parent", spanName, ctx );
            if ( ctx != null )
            {
                spanBuilder.setParent( ctx );
                isRoot = false;
            }
            else
            {
                spanBuilder.setNoParent();
            }
        }

        return startSpan( spanBuilder, SpanKind.INTERNAL, isRoot );
    }

    @Override
    public SpanAdapter startClientSpan( String spanName )
    {
        return startClientSpan( spanName, Optional.empty() );
    }

    @Override
    public SpanAdapter startClientSpan( String spanName, Optional<SpanContext> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        Context ctx = Context.current();
        if ( parentContext.isPresent() )
        {
            ctx = ( (OtelSpanContext) parentContext.get() ).getContext();
            spanBuilder.setParent( ctx );
            logger.trace( "The span {} is using a parent context {}", spanName, ctx );
        }
        boolean localRoot = true;
        logger.trace( "Start a new client span {}", spanName );
        if ( ctx != null )
        {
            logger.trace( "The span {} is using a parent context {}", spanName, ctx );
            spanBuilder.setParent( ctx );
            localRoot = false;
        }
        else
        {
            spanBuilder.setNoParent();
        }
        return startSpan( spanBuilder, SpanKind.CLIENT, localRoot );
    }

    private OtelSpan startSpan( SpanBuilder builder, SpanKind kind, boolean isRoot )
    {
        Span span = builder.setSpanKind( kind ).startSpan();
        try (Scope ignored = span.makeCurrent())
        {
            logger.trace( "span with id {} started in trace {}", span.getSpanContext().getSpanId(),
                          span.getSpanContext().getTraceId() );
        }
        return new OtelSpan( span, isRoot );
    }
}
