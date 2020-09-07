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
package org.commonjava.o11yphant.honeycomb;

import com.codahale.metrics.Snapshot;
import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import org.commonjava.cdi.util.weft.ThreadContextualizer;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static com.codahale.metrics.MetricAttribute.COUNT;
import static com.codahale.metrics.MetricAttribute.MAX;
import static com.codahale.metrics.MetricAttribute.MEAN;
import static com.codahale.metrics.MetricAttribute.MIN;

@ApplicationScoped
@Named
public class HoneycombContextualizer
        implements ThreadContextualizer
{
    private static final String THREAD_NAME = "thread.name";

    private static final String THREAD_GROUP_NAME = "thread.group.name";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final ThreadLocal<Span> SPAN = new ThreadLocal<>();

    private static final ThreadLocal<SpanContext> SPAN_CONTEXT = new ThreadLocal<>();

    @Inject
    private HoneycombManager honeycombManager;

    @Inject
    private HoneycombConfiguration configuration;

    @Inject
    private DefaultTracingContext tracingContext;

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
            SpanContext ctx = SPAN_CONTEXT.get();
            if ( ctx == null )
            {
                Beeline beeline = honeycombManager.getBeeline();
                ctx = new SpanContext( beeline.getActiveSpan() );
                SPAN_CONTEXT.set( ctx );
            }
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
            SpanContext parentSpanContext = (SpanContext) parentContext;
            logger.trace( "Creating thread-level root span using parent-thread context: {}", parentContext );
            SPAN.set( honeycombManager.startRootTracer( "thread." + Thread.currentThread().getThreadGroup().getName(), parentSpanContext ) );
        }
    }

    @Override
    public void clearContext()
    {
        if ( configuration.isEnabled() )
        {
            Span span = SPAN.get();
            if ( span != null )
            {
                logger.trace( "Closing thread-level root span: {}", span );
                honeycombManager.addFields( span );
                span.addField( THREAD_NAME, Thread.currentThread().getName() );
                span.addField( THREAD_GROUP_NAME, Thread.currentThread().getThreadGroup().getName() );

                addSpanContextFields( span );

                span.close();

                honeycombManager.endTrace();
            }

            SPAN.remove();
            SPAN_CONTEXT.remove();

            tracingContext.clearThreadSpans();
        }
    }

    private void addSpanContextFields( Span span )
    {
        SpanContext spanContext = SPAN_CONTEXT.get();
        if ( spanContext != null )
        {
            spanContext.getTimers().forEach( ( k, v ) -> {
                Snapshot st = v.getSnapshot();
                span.addField( COUNT + "." + k, v.getCount() );
                span.addField( MEAN + "." + k, st.getMean() );
                span.addField( MAX + "." + k, st.getMax() );
                span.addField( MIN + "." + k, st.getMin() );
            } );
            spanContext.getMeters().forEach( ( k, v ) -> {
                span.addField( k, v.getCount() );
            } );
        }
    }

}
