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

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
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

    private RootSpanDecorator rootSpanDecorator;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    public TraceManager( O11yphantTracePlugin tracePlugin,
                         RootSpanDecorator rootSpanDecorator )
    {
        this.spanProvider = tracePlugin.getSpanProvider();
        this.contextPropagator = tracePlugin.getContextPropagator();
        this.rootSpanDecorator = rootSpanDecorator;
    }

    public SpanAdapter<T> startServiceRootSpan( String spanName, Optional<SpanContext<T>> parentContext )
    {
        SpanAdapter<T> span = spanProvider.startServiceRootSpan( spanName, parentContext);
        if ( span != null )
        {
            setActiveSpan( span );
        }
        return span;
    }

    public SpanAdapter<T> startChildSpan( final String spanName )
    {
        return startChildSpan( spanName, Optional.empty() );
    }

    public SpanAdapter<T> startChildSpan( final String spanName, Optional<SpanContext<T>> parentContext )
    {
        SpanAdapter<T> span = spanProvider.startChildSpan( spanName, parentContext );
        if ( span != null )
        {
            setActiveSpan( span );
        }
        return span;
    }

    public void addSpanField( String name, Object value )
    {
        SpanAdapter<T> span = getActiveSpan();
        if ( span != null )
        {
            span.addField( name, value );
        }
    }

    public void addStartField( SpanAdapter<T> span, String name, long begin )
    {
        String startFieldName = name( name, REQUEST_PHASE_START );
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.setInProgressField( startFieldName, begin );
    }

    public void addEndField( SpanAdapter<T> span, String name, long end )
    {
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

    public void addCumulativeField( SpanAdapter<T> span, String name, long elapse )
    {
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

    public void addRootSpanFields()
    {
        SpanAdapter<T> span = getActiveSpan();
        if ( span != null )
        {
            addRootSpanFields( span );
        }
    }

    public void addRootSpanFields( SpanAdapter<T> span )
    {
        rootSpanDecorator.decorate( span );
    }

    public Optional<SpanContext<T>> extractContext( HttpServletRequest request )
    {
        return contextPropagator.extractContext( request );
    }

    private void setActiveSpan( SpanAdapter<T> spanAdapter )
    {
        ThreadContext ctx = ThreadContext.getContext( true );
        ctx.put( ACTIVE_SPAN, spanAdapter );
    }

    public SpanAdapter<T> getActiveSpan()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            return (SpanAdapter<T>) ctx.get( ACTIVE_SPAN );
        }

        return null;
    }

}
