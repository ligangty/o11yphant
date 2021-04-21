package org.commonjava.o11yphant.honeycomb;

import io.honeycomb.libhoney.EventPostProcessor;
import org.commonjava.o11yphant.honeycomb.impl.ConfigurableTraceSampler;
import org.commonjava.o11yphant.honeycomb.impl.DefaultTracingContext;
import org.commonjava.o11yphant.honeycomb.impl.SimpleTraceSampler;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.honeycomb.impl.HoneycombContextPropagator;
import org.commonjava.o11yphant.honeycomb.impl.HoneycombSpanProvider;
import org.commonjava.o11yphant.honeycomb.impl.event.MeteringEventPostProcessor;
import org.commonjava.o11yphant.metrics.AbstractTrafficClassifier;
import org.commonjava.o11yphant.metrics.TrafficClassifier;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;

import java.util.Optional;

public class HoneycombTracePlugin implements O11yphantTracePlugin<HoneycombType>
{
    private DefaultTracingContext threadTracingContext;

    private HoneycombContextPropagator contextPropagator;

    private HoneycombSpanProvider spanProvider;

    public HoneycombTracePlugin( TracerConfiguration traceConfig, HoneycombConfiguration hcConfig,
                                 Optional<TrafficClassifier> trafficClassifier )
    {
        this( traceConfig, hcConfig, trafficClassifier, Optional.empty() );
    }

    public HoneycombTracePlugin( TracerConfiguration traceConfig, HoneycombConfiguration hcConfig,
                                 Optional<TrafficClassifier> trafficClassifier,
                                 Optional<EventPostProcessor> eventPostProcessor )
    {
        this.contextPropagator = new HoneycombContextPropagator();
        if ( trafficClassifier.isPresent() )
        {
            this.threadTracingContext = new DefaultTracingContext( traceConfig );
            this.spanProvider = new HoneycombSpanProvider( hcConfig, traceConfig,
                                                           new ConfigurableTraceSampler( trafficClassifier.get(),
                                                                                         traceConfig ),
                                                           threadTracingContext,
                                                           eventPostProcessor );
        }
        else
        {
            this.spanProvider =
                            new HoneycombSpanProvider( hcConfig, traceConfig, new SimpleTraceSampler( traceConfig ) );
        }
    }

    public HoneycombTracePlugin( HoneycombContextPropagator contextPropagator, HoneycombSpanProvider spanProvider )
    {
        this.contextPropagator = contextPropagator;
        this.spanProvider = spanProvider;
    }

    @Override
    public SpanProvider<HoneycombType> getSpanProvider()
    {
        return spanProvider;
    }

    @Override
    public ContextPropagator<HoneycombType> getContextPropagator()
    {
        return contextPropagator;
    }

    @Override
    public ThreadTracingContext<HoneycombType> getThreadTracingContext()
    {
        return threadTracingContext;
    }
}
