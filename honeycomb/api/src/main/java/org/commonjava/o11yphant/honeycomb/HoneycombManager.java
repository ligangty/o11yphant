package org.commonjava.o11yphant.honeycomb;

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
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected HoneycombConfiguration configuration;

    protected TraceSampler traceSampler;

    protected TracingContext tracingContext;

    protected EventPostProcessor eventPostProcessor;

    protected HoneyClient client;

    protected Beeline beeline;

    protected CustomTraceIdProvider getCustomTraceIdProvider()
    {
        return null;
    }

    protected List<RootSpanFields> rootSpanFieldsList = new ArrayList<>();

    public HoneycombManager()
    {
    }

    public HoneycombManager( HoneycombConfiguration configuration, TraceSampler traceSampler )
    {
        this( configuration, traceSampler, null, null );
    }

    public HoneycombManager( HoneycombConfiguration configuration, TraceSampler traceSampler,
                             TracingContext tracingContext, EventPostProcessor eventPostProcessor )
    {
        this.configuration = configuration;
        this.traceSampler = traceSampler;
        this.tracingContext = tracingContext;
        this.eventPostProcessor = eventPostProcessor;
    }

    public void init()
    {
        if ( configuration.isEnabled() )
        {
            String writeKey = configuration.getWriteKey();
            String dataset = configuration.getDataset();

            logger.debug( "Init Honeycomb manager, dataset: {}", dataset );
            Options.Builder builder = LibHoney.options().setDataset( dataset ).setWriteKey( writeKey );

            if ( eventPostProcessor != null )
            {
                builder.setEventPostProcessor( eventPostProcessor );
            }

            Options options = builder.build();

            if ( configuration.isConsoleTransport() )
            {
                client = new HoneyClient( options, new ConsoleTransport( new ResponseObservable() ) );
            }
            else
            {
                client = new HoneyClient( options );
            }

            LibHoney.setDefault( client );

            SpanPostProcessor postProcessor = Tracing.createSpanProcessor( client, Sampling.alwaysSampler() );

            SpanBuilderFactory factory;

            CustomTraceIdProvider traceIdProvider = getCustomTraceIdProvider();
            if ( traceIdProvider != null )
            {
                logger.info( "Init with traceIdProvider: {}", traceIdProvider );
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

    /**
     * Register new {@link RootSpanFields} instances in case CDI is not available in client library.
     */
    public void registerRootSpanFields( RootSpanFields rootSpanFields )
    {
        rootSpanFieldsList.add( rootSpanFields );
    }

    public Beeline getBeeline()
    {
        return beeline;
    }

    public Span getActiveSpan()
    {
        if ( beeline != null )
        {
            return beeline.getActiveSpan();
        }
        return null;
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

                logger.debug( "Starting root span: {} based on parent context: {}, thread: {}", spanName, propContext,
                              Thread.currentThread().getId() );
                span = beeline.getSpanBuilderFactory()
                              .createBuilder()
                              .setParentContext( propContext )
                              .setSpanName( spanName )
                              .setServiceName( configuration.getServiceName() )
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
                              .setServiceName( configuration.getServiceName() )
                              .build();
            }

            span = beeline.getTracer().startTrace( span );

            logger.debug( "Started root span: {} (ID: {}, trace ID: {} and parent: {}, thread: {})", span,
                          span.getSpanId(), span.getTraceId(), span.getParentSpanId(), Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public void endTrace()
    {
        if ( beeline != null )
        {
            logger.debug( "Ending trace: {}", Thread.currentThread().getName() );
            beeline.getTracer().endTrace();
        }
    }

    public Span startChildSpan( final String spanName )
    {
        Beeline beeline = getBeeline();
        if ( beeline != null )
        {
            Span span;
            if ( tracingContext.isEmpty() )
            {
                logger.debug( "Parent span from context: {} is a NO-OP, starting root trace instead in: {}",
                              tracingContext, Thread.currentThread().getId() );
                span = startRootTracer( spanName );
            }
            else
            {
                span = beeline.startChildSpan( spanName );
            }

            logger.debug( "Child span: {} (id: {}, trace: {}, parent: {}, thread: {})", span, span.getSpanId(),
                          span.getTraceId(), span.getParentSpanId(), Thread.currentThread().getId() );

            span.markStart();
            return span;
        }

        return null;
    }

    public void addSpanField( String name, Object value )
    {
        Span  span = getActiveSpan();
        if ( span != null )
        {
            span.addField( name, value );
        }
    }

    public void addRootSpanFields()
    {
        Span span = getActiveSpan();
        if ( span != null )
        {
            addRootSpanFields( span );
        }
    }

    public void addRootSpanFields( Span span )
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

}
