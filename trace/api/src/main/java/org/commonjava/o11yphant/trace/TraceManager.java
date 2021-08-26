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
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.trace.impl.CloseBlockingSpan;
import org.commonjava.o11yphant.trace.impl.FieldInjectionSpan;
import org.commonjava.o11yphant.trace.impl.ThreadedSpan;
import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;
import org.commonjava.o11yphant.trace.thread.TraceThreadContextualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.commonjava.o11yphant.metrics.MetricsConstants.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.MAX_TIME_MS;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

public final class TraceManager<T extends TracerType>
{
    private static final ThreadLocal<SpanAdapter> ACTIVE_SPAN = new ThreadLocal<>();

    private final SpanProvider<T> spanProvider;

    private final ContextPropagator<T> contextPropagator;

    private final SpanFieldsDecorator spanFieldsDecorator;

    private final TracerConfiguration config;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private TraceThreadContextualizer<T> traceThreadContextualizer;

    public TraceManager( O11yphantTracePlugin<T> tracePlugin, SpanFieldsDecorator spanFieldsDecorator,
                         TracerConfiguration config )
    {
        this.spanProvider = tracePlugin.getSpanProvider();
        this.contextPropagator = tracePlugin.getContextPropagator();
        this.traceThreadContextualizer =
                        new TraceThreadContextualizer( config, this, tracePlugin.getThreadTracingContext() );

        this.spanFieldsDecorator = spanFieldsDecorator;
        this.config = config;
    }

    public Optional<SpanAdapter> startClientRequestSpan(String spanName, BiConsumer<String, String> spanInjector )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        SpanAdapter span = spanProvider.startClientSpan( spanName );
        if ( span != null )
        {
            contextPropagator.injectContext( spanInjector, span );
            setActiveSpan( span );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }

            logger.trace( "Started span: {}", span.getSpanId() );
        }

        return span == null ? Optional.empty() : Optional.of( span );
    }

    public Optional<SpanAdapter> startThreadRootSpan( String spanName, ThreadedTraceContext threadedContext )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        Optional<SpanContext<T>> parentContext = contextPropagator.extractContext( threadedContext );
        SpanAdapter span = spanProvider.startServiceRootSpan( spanName, parentContext );
        if ( span != null )
        {
            setActiveSpan( span );

            SpanAdapter finalSpan = span;
            threadedContext.getActiveSpan().ifPresent( parentSpan ->{
                parentSpan.getFields().forEach( (k,v) ->{
                    if ( !( v instanceof Metric ) )
                    {
                        finalSpan.addField( k, v );
                    }
                } );
            } );


            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }

            span = new ThreadedSpan( span, threadedContext.getActiveSpan() );
        }

        logger.trace( "Started span: {}", span.getSpanId() );
        return Optional.of( span );
    }

    public Optional<SpanAdapter> startServiceRootSpan( String spanName, Supplier<Map<String, String>> mapSupplier )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        Optional<SpanContext<T>> parentContext = contextPropagator.extractContext( mapSupplier );
        SpanAdapter span = spanProvider.startServiceRootSpan( spanName, parentContext );
        if ( span != null )
        {
            setActiveSpan( span );

            span = new FieldInjectionSpan( span, spanFieldsDecorator );
        }

        logger.trace( "Started span: {}", span.getSpanId() );
        return Optional.of( span );
    }

    public Optional<SpanAdapter> startChildSpan( final String spanName )
    {
        return startChildSpan( spanName, Optional.empty() );
    }

    // FIXME: Is this parentContext parameter really necessary? Are we leaking state outside the trace manager with this?
    public Optional<SpanAdapter> startChildSpan( final String spanName, Optional<SpanContext<T>> parentContext )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
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

        logger.trace( "Started span: {}", span.getSpanId() );
        return Optional.of( span );
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

        String startFieldName = name;
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.setInProgressField( startFieldName, begin );
    }

    public void addEndField( SpanAdapter span, String name, long end )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        String startFieldName = name;
        Long begin = span.getInProgressField( startFieldName, null );
        if ( begin == null )
        {
            logger.trace( "Failed to get START field, span: {}, name: {}", span, name );
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
        span.setInProgressField( averageName, (double) ( cumulativeMs / cumulativeCount ) );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulative-ms: {}, count: {}", span, name,
                      elapse, cumulativeMs, cumulativeCount );
    }

    private void setActiveSpan( SpanAdapter spanAdapter )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        ACTIVE_SPAN.set(spanAdapter);
    }

    public static Optional<SpanAdapter> getActiveSpan()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            SpanAdapter span = ACTIVE_SPAN.get();
            return span == null ? Optional.empty() : Optional.of( span );
        }

        return Optional.empty();
    }

    public static void addFieldToActiveSpan( String name, Object value )
    {
        Logger logger = LoggerFactory.getLogger( TraceManager.class );

        Optional<SpanAdapter> s = getActiveSpan();
        s.ifPresent( span -> {
            logger.trace( "Adding field: {} with value: {} to span: {}", name, value, span.getSpanId() );
            span.addField( name, value );
        } );

        if ( !s.isPresent() )
        {
            logger.info( "NO ACTIVE SPAN for: {} from: {}", name, Thread.currentThread().getStackTrace()[1] );
        }
    }

    public TraceThreadContextualizer<T> getTraceThreadContextualizer()
    {
        return traceThreadContextualizer;
    }

    public static Optional<SpanAdapter> addCloseBlockingDecorator( Optional<SpanAdapter> span,
                                                                   CloseBlockingDecorator injector )
    {
        if ( span.isPresent() )
        {
            SpanAdapter sa = span.get();
            if ( sa instanceof CloseBlockingSpan )
            {
                ((CloseBlockingSpan) span.get()).addInjector( injector );
                return span;
            }
            else
            {
                return Optional.of( new CloseBlockingSpan( span.get(), injector ) );
            }
        }
        else
        {
            return span;
        }
    }
}
