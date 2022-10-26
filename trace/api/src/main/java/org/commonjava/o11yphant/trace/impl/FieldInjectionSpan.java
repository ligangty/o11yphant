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
import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This span wrapper decorates the span when it is finally closed from a variety of sources. It is possible to add
 * basic field-injection decorators, which will always inject some data when the span is closed, regardless of when
 * that happens.
 *
 * Then, it's also possible to add {@link CloseBlockingDecorator} instances. When these are present, this wrapper is
 * designed to count the number of close() calls and match them to the number of close-blocking decorators present.
 * When the close call count matches this close-blocking decorator count, the span gets closed.
 *
 * The purpose of the close-blocking decorators is to allow something like a REST request to return when a transfer
 * stream is still pending. In that case, you want to add some final span information when the transfer completes,
 * such as a calculated transfer speed measurement over the course of actually doing the transfer.
 * In this case, we don't want the servlet / filter to terminate the span...we want the transfer thread to do it. On
 * the other hand, if we're transferring a very small file and that completes before the servlet / filter-chain can
 * manage to complete, we don't want to lose the span data that the filters might contribute.
 *
 * @see org.commonjava.o11yphant.trace.TraceManager#addCloseBlockingDecorator(Optional, CloseBlockingDecorator)
 * @see CloseBlockingDecorator
 */
public class FieldInjectionSpan
                extends SpanWrapper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final SpanFieldsDecorator spanFieldsDecorator;

    private final List<CloseBlockingDecorator> looseInjectors = new ArrayList<>();

    private final AtomicInteger looseCloseCalls = new AtomicInteger( 0 );

    public FieldInjectionSpan( SpanAdapter delegate, SpanFieldsDecorator spanFieldsDecorator )
    {
        super(delegate);
        this.spanFieldsDecorator = spanFieldsDecorator;
        spanFieldsDecorator.decorateOnStart( delegate );
    }

    public void addInjector( CloseBlockingDecorator injector )
    {
        looseInjectors.add( 0, injector );
    }

    @java.lang.Override
    public void close()
    {
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "SPAN: {}, close() called from: {}. Injectors: {}, previous close call count: {}",
                          getSpanId(), Thread.currentThread().getStackTrace()[3].getClassName(), looseInjectors,
                          looseCloseCalls.get() );
        }

        SpanAdapter delegate = getDelegate();
        if ( !looseInjectors.isEmpty() )
        {
            if ( looseCloseCalls.incrementAndGet() >= looseInjectors.size() )
            {
                logger.trace( "Really closing {} this time. Decorating in preparation...", this );
                looseInjectors.forEach( i -> i.decorateSpanAtClose( delegate ) );
            }
            else
            {
                return;
            }
        }

        logger.trace( "Decorating on close: {}", delegate );
        spanFieldsDecorator.decorateOnClose( delegate );
        logger.trace( "Calling SpanAdapter.close() on: {}", delegate );
        delegate.close();
    }

}
