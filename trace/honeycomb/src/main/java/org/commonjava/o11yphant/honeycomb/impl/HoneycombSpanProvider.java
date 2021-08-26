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

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.SpanBuilderFactory;
import io.honeycomb.beeline.tracing.SpanPostProcessor;
import io.honeycomb.beeline.tracing.Tracer;
import io.honeycomb.beeline.tracing.Tracing;
import io.honeycomb.beeline.tracing.context.TracingContext;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.sampling.Sampling;
import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import io.honeycomb.libhoney.Options;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;
import io.honeycomb.libhoney.transport.impl.ConsoleTransport;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpan;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpanContext;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.honeycomb.HoneycombConfiguration;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HoneycombSpanProvider implements SpanProvider<HoneycombType>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected HoneycombConfiguration honeycombConfiguration;

    protected TracerConfiguration tracerConfiguration;

    protected TraceSampler traceSampler;

    protected TracingContext tracingContext;

    protected EventPostProcessor eventPostProcessor;

    protected HoneyClient client;

    protected Beeline beeline;

    public HoneycombSpanProvider( HoneycombConfiguration configuration, TracerConfiguration tracerConfiguration,
                                  TraceSampler traceSampler )
    {
        this( configuration, tracerConfiguration, traceSampler, null, null );
    }

    public HoneycombSpanProvider( HoneycombConfiguration honeycombConfiguration, TracerConfiguration tracerConfiguration,
                             TraceSampler traceSampler, TracingContext tracingContext,
                             Optional<EventPostProcessor> eventPostProcessor )
    {
        this.tracerConfiguration = tracerConfiguration;
        this.honeycombConfiguration = honeycombConfiguration;
        this.traceSampler = traceSampler;
        this.tracingContext = tracingContext;
        if ( eventPostProcessor.isPresent() )
        {
            this.eventPostProcessor = eventPostProcessor.get();
        }

        init();
    }

    public void init()
    {
        if ( tracerConfiguration.isEnabled() )
        {
            String writeKey = honeycombConfiguration.getWriteKey();
            String dataset = honeycombConfiguration.getDataset();

            logger.debug( "Init Honeycomb manager, dataset: {}", dataset );
            Options.Builder builder = LibHoney.options().setDataset( dataset ).setWriteKey( writeKey );

            if ( eventPostProcessor != null )
            {
                builder.setEventPostProcessor( eventPostProcessor );
            }

            Options options = builder.build();

            if ( honeycombConfiguration.isConsoleTransport() )
            {
                client = new HoneyClient( options, new ConsoleTransport( new ResponseObservable() ) );
            }
            else
            {
                client = new HoneyClient( options );
            }

            LibHoney.setDefault( client );

            SpanPostProcessor postProcessor = Tracing.createSpanProcessor( client, traceSampler );

            SpanBuilderFactory factory;

            CustomTraceIdProvider traceIdProvider = getCustomTraceIdProvider();
            if ( traceIdProvider != null )
            {
                logger.trace( "Init with traceIdProvider: {}", traceIdProvider );
                factory = new SpanBuilderFactory( postProcessor, SystemClockProvider.getInstance(), traceIdProvider,
                                                  traceSampler );
            }
            else
            {
                factory = Tracing.createSpanBuilderFactory( postProcessor, traceSampler );
            }

            Tracer tracer;
            if ( tracingContext != null )
            {
                tracer = Tracing.createTracer( factory, tracingContext );
            }
            else
            {
                tracer = Tracing.createTracer( factory );
            }

            beeline = Tracing.createBeeline( tracer, factory );
        }
    }

    protected CustomTraceIdProvider getCustomTraceIdProvider()
    {
        return null;
    }

    @Override
    public SpanAdapter startServiceRootSpan( String spanName, Optional<SpanContext<HoneycombType>> parentContext )
    {
        if ( beeline != null )
        {
            Span span;
            if ( parentContext.isPresent() )
            {
                HoneycombSpanContext parent = (HoneycombSpanContext) parentContext.get();

                // The spanId identifies the latest Span in the trace and will become the parentSpanId of the next Span in the trace.
                PropagationContext propContext = parent.getPropagationContext();

                logger.trace( "Starting root span: {} based on parent context: {}, thread: {}", spanName, propContext,
                              Thread.currentThread().getId() );
                span = beeline.getSpanBuilderFactory()
                              .createBuilder()
                              .setParentContext( propContext )
                              .setSpanName( spanName )
                              .setServiceName( tracerConfiguration.getServiceName() )
                              .build();
            }
            else
            {
/*
                String traceId = RequestContextHelper.getContext( TRACE_ID );
                String parentId = RequestContextHelper.getContext( REQUEST_PARENT_SPAN );
*/
                span = beeline.getSpanBuilderFactory()
                              .createBuilder()
                              .setSpanName( spanName )
                              .setServiceName( tracerConfiguration.getServiceName() )
                              .build();

            }

            span = beeline.getTracer().startTrace( span );

            logger.trace( "Started root span: {} (ID: {}, trace ID: {} and parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(), Thread.currentThread().getId() );

            span.markStart();
            return new HoneycombSpan( span, beeline );
        }

        return null;
    }

    @Override
    public SpanAdapter startChildSpan( String spanName, Optional<SpanContext<HoneycombType>> parentContext )
    {
        if ( beeline != null )
        {
            if ( tracingContext != null && tracingContext.isEmpty() )
            {
                logger.trace( "Parent span from context: {} is a NO-OP, starting root trace instead in: {}",
                              tracingContext, Thread.currentThread().getId() );
                return startServiceRootSpan( spanName, Optional.empty() );
            }

            Span span = beeline.startChildSpan( spanName );

            logger.trace( "Child span: {} (id: {}, trace: {}, parent: {}, thread: {})", span, span.getSpanId(),
                          span.getTraceId(), span.getParentSpanId(), Thread.currentThread().getId() );

            span.markStart();
            return new HoneycombSpan( span, beeline );
        }

        return null;
    }

    @Override
    public SpanAdapter startClientSpan( String spanName )
    {
        if ( beeline != null )
        {
            if ( tracingContext != null && tracingContext.isEmpty() )
            {
                logger.trace( "Parent span from context: {} is a NO-OP, starting root trace instead in: {}",
                              tracingContext, Thread.currentThread().getId() );
                return startServiceRootSpan( spanName, Optional.empty() );
            }

            Span span = beeline.startChildSpan( spanName );

            logger.trace( "Child span: {} (id: {}, trace: {}, parent: {}, thread: {})", span, span.getSpanId(),
                          span.getTraceId(), span.getParentSpanId(), Thread.currentThread().getId() );

            span.markStart();
            return new HoneycombSpan( span, beeline );
        }

        return null;
    }

}
