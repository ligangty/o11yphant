package org.commonjava.o11yphant.honeycomb;

import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.honeycomb.impl.HoneycombContextPropagator;
import org.commonjava.o11yphant.honeycomb.impl.HoneycombSpanProvider;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;

public class HoneycombTracePlugin implements O11yphantTracePlugin<HoneycombType>
{
    private HoneycombContextPropagator contextPropagator;

    private HoneycombSpanProvider spanProvider;

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
}
