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

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.SpanBuilderFactory;
import io.honeycomb.beeline.tracing.SpanPostProcessor;
import io.honeycomb.beeline.tracing.Tracer;
import io.honeycomb.beeline.tracing.Tracing;
import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.sampling.Sampling;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapper;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperNamed;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperNamedAfterRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.commonjava.o11yphant.metrics.MetricsConstants.AVERAGE_TIME_MS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.MAX_TIME_MS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REQUEST_PARENT_SPAN;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REQUEST_PHASE_START;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.TRACE_ID;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.getContext;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private HoneyClient client;

    private Beeline beeline;

    @Inject
    private HoneycombContextualizer honeycombContextualizer;

    @Inject
    private HoneycombConfiguration configuration;

    @Inject
    private DefaultTraceSampler traceSampler;

    @Inject
    private DefaultTracingContext tracingContext;

    @Inject
    private EventPostProcessor eventPostProcessor;

    @Inject
    private Instance<RootSpanFields> rootSpanFieldsInstance;

    private List<RootSpanFields> rootSpanFieldsList = new ArrayList<>();

    public HoneycombManager()
    {
    }

    @PostConstruct
    public void init()
    {
        if ( configuration.isEnabled() )
        {
            String writeKey = configuration.getWriteKey();
            String dataset = configuration.getDataset();

            logger.debug( "Init Honeycomb manager, dataset: {}", dataset );
            client = new HoneyClient( LibHoney.options().setDataset( dataset ).setWriteKey( writeKey )
                                              .setEventPostProcessor( eventPostProcessor ).build() ); //, new ConsoleTransport( new ResponseObservable() ) );
            LibHoney.setDefault( client );

            SpanPostProcessor postProcessor = Tracing.createSpanProcessor( client, Sampling.alwaysSampler() );
            SpanBuilderFactory factory = Tracing.createSpanBuilderFactory( postProcessor, traceSampler );

            Tracer tracer = Tracing.createTracer( factory, tracingContext );
            beeline = Tracing.createBeeline( tracer, factory );

            rootSpanFieldsInstance.forEach( instance -> rootSpanFieldsList.add( instance ) );
        }
    }

    /**
     * Register new {@link RootSpanFields} instances in case CDI is not available in client library.
     */
    public void registerRootSpanFields( RootSpanFields rootSpanFields )
    {
        rootSpanFieldsList.add( rootSpanFields );
    }

    public HoneyClient getClient()
    {
        return client;
    }

    public Beeline getBeeline()
    {
        return beeline;
    }

    public Span startRootTracer( String spanName )
    {
        return startRootTracer( spanName, null );
    }

    public Span startRootTracer( String spanName, SpanContext parentContext )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span;
            if ( parentContext != null )
            {
                // The spanId identifies the latest Span in the trace and will become the parentSpanId of the next Span in the trace.
                PropagationContext propContext =
                                new PropagationContext( parentContext.getTraceId(), parentContext.getSpanId(), null,
                                                        null );

                logger.debug( "Starting root span: {} based on parent context: {}, thread: {}", spanName, propContext, Thread.currentThread().getId() );
                span = beeline.getSpanBuilderFactory()
                              .createBuilder()
                              .setParentContext( propContext )
                              .setSpanName( spanName )
                              .setServiceName( configuration.getServiceName() )
                              .build();
            }
            else
            {
                String traceId = RequestContextHelper.getContext( TRACE_ID );
                String parentId = RequestContextHelper.getContext( REQUEST_PARENT_SPAN );

                span = beeline.getSpanBuilderFactory().createBuilder()
                              //.setParentContext( parentContext )
                              .setSpanName( spanName ).setServiceName( configuration.getServiceName() ).build();
            }

            span = beeline.getTracer().startTrace( span );

            logger.debug( "Started root span: {} (ID: {}, trace ID: {} and parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(),
                          Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public Span startChildSpan( final String spanName )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span;
            if ( tracingContext.isEmpty() )
            {
                logger.debug( "Parent span from context: {} is a NO-OP, starting root trace instead in: {}", tracingContext, Thread.currentThread().getId() );
                span = startRootTracer( spanName );
            }
            else
            {
                span = beeline.startChildSpan( spanName );
            }

            logger.debug( "Child span: {} (id: {}, trace: {}, parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(),
                          Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public void addFields( Span span )
    {
        if ( beeline != null )
        {
            ThreadContext ctx = ThreadContext.getContext( false );
            if ( ctx != null )
            {
                configuration.getFieldSet().forEach( field -> {
                    Object value = getContext( field );
                    if ( value != null )
                    {
                        span.addField( field, value );
                    }
                } );

                Map<String, Double> cumulativeTimings = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );
                if ( cumulativeTimings != null )
                {
                    cumulativeTimings.forEach( ( k, v ) -> span.addField( CUMULATIVE_TIMINGS + "." + k, v ) );
                }

                Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNT );
                if ( cumulativeCounts != null )
                {
                    cumulativeCounts.forEach( ( k, v ) -> span.addField( CUMULATIVE_COUNT + "." + k, v ) );
                }
            }

            addRootSpanFields( span ); // add custom root span fields via RootSpanFields
        }
    }

    private void addRootSpanFields( Span span )
    {
        rootSpanFieldsList.forEach( rootSpanFields -> {
            Map<String, Object> fields = rootSpanFields.get();
            if ( fields != null )
            {
                fields.forEach( ( k, v ) -> span.addField( k, v ) );
            }
            logger.debug( "Add root span fields for: {}, fields: {}", rootSpanFields, fields );
        } );
    }

    public void endTrace()
    {
        if ( beeline != null )
        {
            logger.debug( "Ending trace: {}", Thread.currentThread().getId() );
            getBeeline().getTracer().endTrace();
        }
    }

    public Span getActiveSpan()
    {
        if ( beeline != null )
        {
            return beeline.getActiveSpan();
        }
        return null;
    }

    public Optional<Timer> getSpanTimer( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Timer timer = spanContext.getTimer( name );
            if ( timer == null )
            {
                timer = new Timer();
                spanContext.putTimer( name, timer );
            }
            return Optional.of( timer );
        }
        return Optional.empty();
    }

    public Optional<Timer.Context> startSpanTimer( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Timer timer = getSpanTimer( name ).get();
            if ( timer != null )
            {
                return Optional.of( timer.time() );
            }
        }
        return Optional.empty();
    }

    public Optional<Meter> getSpanMeter( String name )
    {
        SpanContext spanContext = (SpanContext) honeycombContextualizer.extractCurrentContext();
        if ( spanContext != null )
        {
            Meter meter = spanContext.getMeter( name );
            if ( meter == null )
            {
                meter = new Meter();
                spanContext.putMeter( name, meter );
            }
            return Optional.of( meter );
        }
        return Optional.empty();
    }

    public void addSpanField( String name, Object value )
    {
        Span  span = getActiveSpan();
        if ( span != null )
        {
            span.addField( name, value );
        }
    }

    /**
     * Add cumulative fields to specified span, i.e., cumulative-timings/count/avg/max.
     */
    public void addCumulativeField( Span span, String name, long elapse )
    {
        final Map<String, Object> fields = span.getFields();

        // cumulative timing
        String cumulativeTimingName = name( name, CUMULATIVE_TIMINGS );
        Long cumulativeMs = (Long) fields.getOrDefault( cumulativeTimingName, 0L );
        cumulativeMs += elapse;
        span.addField( cumulativeTimingName, cumulativeMs );

        // cumulative count
        String cumulativeCountName = name( name, CUMULATIVE_COUNT );
        Integer cumulativeCount = (Integer) fields.getOrDefault( cumulativeCountName, 0 );
        cumulativeCount += 1;
        span.addField( cumulativeCountName, cumulativeCount );

        // max
        String maxTimingName = name( name, MAX_TIME_MS );
        Long max = (Long) fields.getOrDefault( maxTimingName, 0L );
        if ( elapse > max )
        {
            span.addField( maxTimingName, elapse );
        }

        // average
        String averageName = name( name, AVERAGE_TIME_MS );
        span.addField( averageName, ( cumulativeMs / cumulativeCount ) );

        logger.trace( "addCumulativeField, span: {}, name: {}, elapse: {}, cumulative-ms: {}, count: {}", span, name,
                      elapse, cumulativeMs, cumulativeCount );
    }

    public void addStartField( Span span, String name, long begin )
    {
        String startFieldName = name( name, REQUEST_PHASE_START );
        logger.trace( "addStartField, span: {}, name: {}, begin: {}", span, name, begin );
        span.addField( startFieldName, begin );
    }

    public void addEndField( Span span, String name, long end )
    {
        String startFieldName = name( name, REQUEST_PHASE_START );
        Long begin = (Long) span.getFields().get( startFieldName );
        if ( begin == null )
        {
            logger.warn( "Failed to get START field, span: {}, name: {}", span, name );
            return;
        }
        logger.trace( "addEndField, span: {}, name: {}, end: {}", span, name, end );
        long elapse = end - begin;
        addCumulativeField( span, name, elapse );
        span.addField( startFieldName, null ); // clear start field
    }


    /**
     * Wrap method with standard {@link MetricWrapper}. The HoneycombWrapperInterceptor will pick it up
     * to add cumulative field. The field name is a combination of classifier which is the full name, e.g, with nodeId prefix,
     * plus the appendix which is calculated after the execution of the method.
     * The appendix gives caller a chance to append additional token to the name, e.g., the HTTP response code, etc.
     */
    @MetricWrapper
    public <T> T withStandardMetricWrapper( final Supplier<T> method,
                                            @MetricWrapperNamed final Supplier<String> classifier,
                                            @MetricWrapperNamedAfterRun final Supplier<String> appendix )
    {
        return method.get();
    }

}
