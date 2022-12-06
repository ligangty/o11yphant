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

import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.trace.impl.FieldInjectionSpan;
import org.commonjava.o11yphant.trace.impl.SpanWrapper;
import org.commonjava.o11yphant.trace.impl.ThreadedSpan;
import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.commonjava.o11yphant.metrics.MetricsConstants.*;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

public final class TraceManager<T extends TracerType>
{
    private static final ThreadLocal<Queue<SpanAdapter>> ACTIVE_SPAN = new ThreadLocal<>();

    //    private static final String ACTIVE_SPAN_KEY = "active-trace-span";

    private final SpanProvider<T> spanProvider;

    private final ContextPropagator<T> contextPropagator;

    private final SpanFieldsDecorator spanFieldsDecorator;

    private final TracerConfiguration config;

    private final Logger logger = LoggerFactory.getLogger( getClass().getName() );

    private final TraceThreadContextualizer<T> traceThreadContextualizer;

    public TraceManager( O11yphantTracePlugin<T> tracePlugin, SpanFieldsDecorator spanFieldsDecorator,
                         TracerConfiguration config )
    {
        this.spanProvider = tracePlugin.getSpanProvider();
        this.contextPropagator = tracePlugin.getContextPropagator();
        //noinspection unchecked,rawtypes
        this.traceThreadContextualizer =
                new TraceThreadContextualizer( config, this, tracePlugin.getThreadTracingContext() );

        this.spanFieldsDecorator = spanFieldsDecorator;
        this.config = config;
    }

    public Optional<SpanAdapter> startClientRequestSpan( String spanName, BiConsumer<String, String> spanInjector )
    {
        if ( !config.isEnabled() )
        {
            return Optional.empty();
        }

        logger.trace( "Setting up client span: {}, ACTIVE_SPAN is: {}", spanName, ACTIVE_SPAN.get() );
        SpanAdapter span = spanProvider.startClientSpan( spanName );
        if ( span != null )
        {
            contextPropagator.injectContext( spanInjector, span );
            span = new DeactivationSpan( span );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }

            setActiveSpan( span );

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
            span = new DeactivationSpan( span );

            SpanAdapter finalSpan = span;
            threadedContext.getActiveSpan().ifPresent( parentSpan -> parentSpan.getFields().forEach( ( k, v ) -> {
                if ( !( v instanceof Metric ) )
                {
                    finalSpan.addField( k, v );
                }
            } ) );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }

            span = new ThreadedSpan( span );
            setActiveSpan( span );
            logger.trace( "Started span: {}", span.getSpanId() );
        }

        return span == null ? Optional.empty() : Optional.of( span );
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
            span = new DeactivationSpan( span );

            span = new FieldInjectionSpan( span, spanFieldsDecorator );
            setActiveSpan( span );
            logger.trace( "Started span: {}", span.getSpanId() );
        }

        return span == null ? Optional.empty() : Optional.of( span );
    }

    @SuppressWarnings( "unused" )
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
            span = new DeactivationSpan( span );

            if ( span.isLocalRoot() )
            {
                span = new FieldInjectionSpan( span, spanFieldsDecorator );
            }
            setActiveSpan( span );
            logger.trace( "Started span: {}", span.getSpanId() );
        }

        return span == null ? Optional.empty() : Optional.of( span );
    }

    public void addStartField( SpanAdapter span, String name, double begin )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.setInProgressField( name, begin );
    }

    public void addEndField( SpanAdapter span, String name, long end )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        Double begin = span.getInProgressField( name, null );
        if ( begin == null )
        {
            logger.trace( "Failed to get START field, span: {}, name: {}", span, name );
            return;
        }
        logger.trace( "addEndField, span: {}, name: {}, end: {}", span, name, end );
        double elapse = end - begin;
        addCumulativeField( span, name, elapse );
        span.clearInProgressField( name );
    }

    public void addCumulativeField( SpanAdapter span, String name, double elapse )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        // cumulative timing
        String cumulativeTimingName = name( name, CUMULATIVE_TIMINGS );
        Double cumulativeMs = span.updateInProgressField( cumulativeTimingName, elapse );

        // cumulative count
        String cumulativeCountName = name( name, CUMULATIVE_COUNT );
        Double cumulativeCount = span.updateInProgressField( cumulativeCountName, 1.0 );

        // max
        String maxTimingName = name( name, MAX_TIME_MS );
        Double max = span.getInProgressField( maxTimingName, 0.0 );
        if ( elapse > max )
        {
            span.setInProgressField( maxTimingName, elapse );
        }

        // average
        String averageName = name( name, AVERAGE_TIME_MS );
        span.setInProgressField( averageName, cumulativeMs / cumulativeCount );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulative-ms: {}, count: {}", span, name,
                      elapse, cumulativeMs, cumulativeCount );
    }

    private void setActiveSpan( SpanAdapter spanAdapter )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        if ( ACTIVE_SPAN.get() == null )
        {
            ACTIVE_SPAN.set( new ConcurrentLinkedQueue<>() );
        }

        ACTIVE_SPAN.get().add( spanAdapter );
    }

    public static void clearThreadSpans()
    {
        if ( ACTIVE_SPAN.get() == null )
        {
            return;
        }

        ACTIVE_SPAN.get().forEach( SpanAdapter::close );
        ACTIVE_SPAN.remove();
    }

    public static Optional<SpanAdapter> getActiveSpan()
    {
        if ( ACTIVE_SPAN.get() == null )
        {
            return Optional.empty();
        }

        SpanAdapter span = ACTIVE_SPAN.get().peek();
        return span == null ? Optional.empty() : Optional.of( span );
    }

    public static void addFieldToActiveSpan( String name, Object value )
    {
        Logger logger = LoggerFactory.getLogger( TraceManager.class );

        Optional<SpanAdapter> s = getActiveSpan();
        s.ifPresent( span -> {
            if ( logger.isTraceEnabled() )
            {
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                logger.trace( "Adding field: {} with value: {} to span: {} from:\n  {}\n  {}", name, value,
                              span.getSpanId(), st[3], st[4] );
            }

            span.addField( name, value );
        } );

        if ( !s.isPresent() && logger.isTraceEnabled() )
        {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            logger.info( "NO ACTIVE SPAN for: {} from:\n  {}\n  {}", name, st[2], st[3] );
        }
    }

    @SuppressWarnings( "unused" )
    public TraceThreadContextualizer<T> getTraceThreadContextualizer()
    {
        return traceThreadContextualizer;
    }

    public static void addCloseBlockingDecorator( Optional<SpanAdapter> span, CloseBlockingDecorator injector )
    {
        Logger logger = LoggerFactory.getLogger( TraceManager.class );

        if ( span.isPresent() )
        {
            SpanAdapter sa = span.get();
            if ( sa instanceof FieldInjectionSpan )
            {
                if ( logger.isTraceEnabled() )
                {
                    StackTraceElement[] st = Thread.currentThread().getStackTrace();
                    logger.trace( "{} Adding close-blocking field injector to: {} from:\n  {}\n  {}", sa.getSpanId(),
                                  sa, st[2], st[3] );
                }

                ( (FieldInjectionSpan) sa ).addInjector( injector );
            }
            else if ( logger.isTraceEnabled() )
            {
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                logger.trace( "{} CANNOT ADD close-blocking field injector around: {} from:\n  {}\n  {}",
                              sa.getSpanId(), sa, st[2], st[3] );
            }
        }
    }

    private class DeactivationSpan
            extends SpanWrapper
    {
        public DeactivationSpan( SpanAdapter delegate )
        {
            super( delegate );
        }

        public void close()
        {
            getDelegate().close();
            Optional<SpanAdapter> active = getActiveSpan();
            SpanAdapter mine = getBaseInstance();
            if ( active.isPresent() && active.get().getBaseInstance() == mine )
            {
                logger.trace( "Clearing active span from TraceManager: {}", active );
                ACTIVE_SPAN.get().remove();
                if ( ACTIVE_SPAN.get().isEmpty() )
                {
                    ACTIVE_SPAN.remove();
                }
            }
            else
            {
                logger.warn(
                        "TraceManager active span does not match the span we expected!\n  Expected: {}\n  Active: {}",
                        mine, active.isPresent() ? active.get().getBaseInstance() : "NULL" );
            }
        }
    }
}
