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
package org.commonjava.o11yphant.trace;

import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.trace.impl.FieldInjectionSpan;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.commonjava.o11yphant.metrics.MetricsConstants.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.MAX_TIME_MS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_PHASE_START;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

public final class TraceManager<T extends TracerType>
{
    public static final String ACTIVE_SPAN = TraceManager.class.getSimpleName() + ".active-span";

    private final SpanProvider<T> spanProvider;

    private final ContextPropagator<T> contextPropagator;

    private final SpanFieldsDecorator spanFieldsDecorator;

    private final TracerConfiguration config;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    public TraceManager( O11yphantTracePlugin<T> tracePlugin, SpanFieldsDecorator spanFieldsDecorator,
                         TracerConfiguration config )
    {
        this.spanProvider = tracePlugin.getSpanProvider();
        this.contextPropagator = tracePlugin.getContextPropagator();
        this.spanFieldsDecorator = spanFieldsDecorator;
        this.config = config;
    }

    public SpanAdapter startClientRequestSpan(String spanName, HttpUriRequest request )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        SpanAdapter span = spanProvider.startClientSpan( spanName );
        contextPropagator.injectContext( request, span );
        setActiveSpan( span );

        if ( span.isLocalRoot() )
        {
            span = new FieldInjectionSpan( span, spanFieldsDecorator );
        }

        return span;
    }

    public SpanAdapter startServiceRootSpan( String spanName, HttpServletRequest hsr )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        Optional<SpanContext<T>> parentContext = contextPropagator.extractContext( hsr );
        SpanAdapter span = spanProvider.startServiceRootSpan( spanName, parentContext );
        if ( span != null )
        {
            setActiveSpan( span );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }
        }

        return span;
    }

    public SpanAdapter startChildSpan( final String spanName )
    {
        return startChildSpan( spanName, Optional.empty() );
    }

    // FIXME: Is this parentContext parameter really necessary? Are we leaking state outside the trace manager with this?
    public SpanAdapter startChildSpan( final String spanName, Optional<SpanContext<T>> parentContext )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        SpanAdapter span = spanProvider.startChildSpan( spanName, parentContext );
        if ( span != null )
        {
            setActiveSpan( span );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }
        }

        return span;
    }

    public void addSpanField( String name, Object value )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        getActiveSpan().ifPresent( span -> span.addField( name, value ) );
    }

    public void addStartField( SpanAdapter span, String name, long begin )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        String startFieldName = name( name, REQUEST_PHASE_START );
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.setInProgressField( startFieldName, begin );
    }

    public void addEndField( SpanAdapter span, String name, long end )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        String startFieldName = name( name, REQUEST_PHASE_START );
        Long begin = span.getInProgressField( startFieldName, null );
        if ( begin == null )
        {
            logger.warn( "Failed to get START field, span: {}, name: {}", span, name );
            return;
        }
        logger.trace( "addEndField, span: {}, name: {}, end: {}", span, name, end );
        long elapse = end - begin;
        addCumulativeField( span, name, elapse );
        span.clearInProgressField( startFieldName );
    }

    public void addCumulativeField( SpanAdapter span, String name, long elapse )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        // cumulative timing
        String cumulativeTimingName = name( name, CUMULATIVE_TIMINGS );
        Long cumulativeMs = span.getInProgressField( cumulativeTimingName, 0L );
        cumulativeMs += elapse;
        span.setInProgressField( cumulativeTimingName, cumulativeMs );

        // cumulative count
        String cumulativeCountName = name( name, CUMULATIVE_COUNT );
        Integer cumulativeCount = span.getInProgressField( cumulativeCountName, 0 );
        cumulativeCount += 1;
        span.setInProgressField( cumulativeCountName, cumulativeCount );

        // max
        String maxTimingName = name( name, MAX_TIME_MS );
        Long max = span.getInProgressField( maxTimingName, 0L );
        if ( elapse > max )
        {
            span.setInProgressField( maxTimingName, elapse );
        }

        // average
        String averageName = name( name, AVERAGE_TIME_MS );
        span.setInProgressField( averageName, ( cumulativeMs / cumulativeCount ) );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulative-ms: {}, count: {}", span, name,
                      elapse, cumulativeMs, cumulativeCount );
    }

    public Optional<SpanContext<T>> extractContext( HttpServletRequest request )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        return contextPropagator.extractContext( request );
    }

    private void setActiveSpan( SpanAdapter spanAdapter )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        ThreadContext ctx = ThreadContext.getContext( true );
        ctx.put( ACTIVE_SPAN, spanAdapter );
    }

    public Optional<SpanAdapter> getActiveSpan()
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            return Optional.of( (SpanAdapter) ctx.get( ACTIVE_SPAN ) );
        }

        return Optional.empty();
    }

}
