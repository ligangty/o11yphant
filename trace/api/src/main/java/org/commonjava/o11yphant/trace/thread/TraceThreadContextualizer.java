package org.commonjava.o11yphant.trace.thread;

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

import org.commonjava.cdi.util.weft.ThreadContextualizer;
import org.commonjava.o11yphant.metrics.api.Snapshot;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.codahale.metrics.MetricAttribute.COUNT;
import static com.codahale.metrics.MetricAttribute.MAX;
import static com.codahale.metrics.MetricAttribute.MEAN;
import static com.codahale.metrics.MetricAttribute.MIN;

public class TraceThreadContextualizer<T extends TracerType>
                implements ThreadContextualizer
{
    private static final String THREAD_NAME = "thread.name";

    private static final String THREAD_GROUP_NAME = "thread.group.name";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final ThreadLocal<Optional<SpanAdapter>> SPAN = new ThreadLocal<>();

    private static final ThreadLocal<ThreadedTraceContext> TRACE_CONTEXT = new ThreadLocal<>();

    private TraceManager<T> traceManager;

    private TracerConfiguration configuration;

    private ThreadTracingContext<T> tracingContext;

    public TraceThreadContextualizer( TracerConfiguration configuration, TraceManager<T> traceManager,
                                      ThreadTracingContext<T> tracingContext )
    {
        this.traceManager = traceManager;
        this.configuration = configuration;
        this.tracingContext = tracingContext;
    }

    @Override
    public String getId()
    {
        return "honeycomb.threadpool.spanner";
    }

    @Override
    public Object extractCurrentContext()
    {
        if ( configuration.isEnabled() )
        {
            Optional<SpanAdapter> activeSpan = traceManager.getActiveSpan();
            ThreadedTraceContext ctx = new ThreadedTraceContext( activeSpan );
            logger.trace( "Extracting parent-thread context: {}", ctx );
            return ctx;
        }
        return null;
    }

    @Override
    public void setChildContext( final Object parentContext )
    {
        if ( configuration.isEnabled() )
        {
            tracingContext.reinitThreadSpans();
            ThreadedTraceContext parentSpanContext = (ThreadedTraceContext) parentContext;
            TRACE_CONTEXT.set( parentSpanContext );

            logger.trace( "Creating thread-level root span using parent-thread context: {}", parentContext );

            Optional<SpanAdapter> threadSpan = traceManager.startThreadRootSpan( "thread." + Thread.currentThread().getThreadGroup().getName(),
                                                        parentSpanContext );
            SPAN.set( threadSpan );

            // TODO: We'd like to propagate normal fields, but metrics that are part of the context should only accumulate
            // back to the parent span, not downward to the threads / children. We need a way to tell the difference,
            // so we're only propagating the non-metric fields downward.
//            Optional<SpanAdapter> parentSpan = parentSpanContext.getActiveSpan();
//            parentSpan.ifPresent( span -> span.getFields().forEach( threadSpan::addField ) );
        }
    }

    @Override
    public void clearContext()
    {
        if ( configuration.isEnabled() )
        {
            Optional<SpanAdapter> span = SPAN.get();
            if ( span != null )
            {
                logger.trace( "Closing thread-level root span: {}", span );
                span.ifPresent( s->{
                    s.addField( THREAD_NAME, Thread.currentThread().getName() );
                    s.addField( THREAD_GROUP_NAME, Thread.currentThread().getThreadGroup().getName() );

                    addSpanContextFields( s );

                    s.close();
                } );
            }

            SPAN.remove();
            TRACE_CONTEXT.remove();

            tracingContext.clearThreadSpans();
        }
    }

    private void addSpanContextFields( SpanAdapter span )
    {
        ThreadedTraceContext threadedTraceContext = TRACE_CONTEXT.get();
        if ( threadedTraceContext != null )
        {
            // TODO: Where are these used?
            // TODO: I think this is meant to accumulate metric fields upward to the parent span from the child/thread.
            // To enable that, we need a way to keep the metric fields separate from informational fields, then
            // call a method to accumulate those upward when the thread span closes.
            threadedTraceContext.getTimers().forEach( ( k, v ) -> {
                Snapshot st = v.getSnapshot();
                span.addField( COUNT + "." + k, v.getCount() );
                span.addField( MEAN + "." + k, st.getMean() );
                span.addField( MAX + "." + k, st.getMax() );
                span.addField( MIN + "." + k, st.getMin() );
            } );

            threadedTraceContext.getMeters().forEach( ( k, v ) -> {
                span.addField( k, v.getCount() );
            } );
        }
    }

}

