package org.commonjava.o11yphant.honeycomb.impl.adapter;

import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;

public class HoneycombSpanContext implements SpanContext<HoneycombType>
{
    private PropagationContext ctx;

    public HoneycombSpanContext( PropagationContext ctx )
    {
        this.ctx = ctx;
    }

    public PropagationContext getPropagationContext()
    {
        return ctx;
    }
}
