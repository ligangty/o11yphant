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
package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.TracerSpan;
import io.honeycomb.beeline.tracing.context.TracingContext;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.honeycomb.HoneycombTracePlugin;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpan;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpanContext;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class DefaultTracingContext
        implements TracingContext, ThreadTracingContext<HoneycombType>
{
    private static ThreadLocal<Deque<TracerSpan>> SPANS = ThreadLocal.withInitial( ArrayDeque::new );

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private TracerConfiguration config;

    public DefaultTracingContext( TracerConfiguration config )
    {
        this.config = config;
    }

    public void reinitThreadSpans()
    {
        if ( config.isEnabled() )
        {
            logger.debug( "Clearing spans in current thread: {}", Thread.currentThread().getId() );
            SPANS.set( new ArrayDeque<>() );
        }
    }

    public void clearThreadSpans()
    {
        if ( config.isEnabled() )
        {
            logger.debug( "Clearing context...SPANs in current thread: {} (thread: {})", SPANS.get().size(),
                         Thread.currentThread().getId() );
            TracerSpan tracerSpan = SPANS.get().peekLast();
            if ( tracerSpan != null )
            {
                tracerSpan.close();
            }

            logger.debug( "Clearing spans deque in: {}", Thread.currentThread().getId() );
            SPANS.remove();
        }
    }

    @Override
    public Deque<TracerSpan> get()
    {
        return SPANS.get();
    }

    @Override
    public int size()
    {
        logger.debug( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId() );
        return SPANS.get().size();
    }

    @Override
    public TracerSpan peekLast()
    {
        logger.debug( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        return SPANS.get().peekLast();
    }

    @Override
    public TracerSpan peekFirst()
    {
        logger.debug( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId() );
        return SPANS.get().peekFirst();
    }

    @Override
    public boolean isEmpty()
    {
        Deque<TracerSpan> spans = SPANS.get();
        logger.debug( "SPANs in current thread: {} (thread: {})", spans.size(), Thread.currentThread().getId() );
        boolean empty = spans.isEmpty();

        logger.debug( "SPANs.isEmpty() ? {}", empty );
        return empty;
    }

    @Override
    public void push( final TracerSpan span )
    {
        logger.debug( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        SPANS.get().push( span );
    }

    @Override
    public TracerSpan pop()
    {
        logger.debug( "Pre-POP SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );

        TracerSpan span = SPANS.get().pop();

        logger.debug( "Post-POP SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        return span;
    }
}
