package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.propagation.W3CPropagationCodec;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpan;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpanContext;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class HoneycombContextPropagator implements ContextPropagator<HoneycombType>
{
    @Override
    public Optional<SpanContext<HoneycombType>> extractContext( Supplier<Map<String, String>> headerSupplier )
    {
        PropagationContext context = W3CPropagationCodec.getInstance().decode( headerSupplier.get() );

        return context.isTraced() ?
                        Optional.of( new HoneycombSpanContext(
                                        new PropagationContext( context.getTraceId(), context.getSpanId(), null,
                                                                null ) ) ) :
                        Optional.empty();
    }

    @Override
    public void injectContext( BiConsumer<String, String> injectorFunction, SpanAdapter spanAdapter )
    {
        HoneycombSpan span = (HoneycombSpan) spanAdapter;
        Optional<Map<String, String>> propMap = W3CPropagationCodec.getInstance().encode( span.getPropagationContext() );
        propMap.ifPresent( stringStringMap -> stringStringMap.forEach( injectorFunction ) );
    }

    @Override
    public Optional<SpanContext<HoneycombType>> extractContext( ThreadedTraceContext threadedContext )
    {
        if ( threadedContext != null && threadedContext.getActiveSpan().isPresent() )
        {
            HoneycombSpan span = (HoneycombSpan) threadedContext.getActiveSpan().get();
            return Optional.of( new HoneycombSpanContext(
                            new PropagationContext( span.getTraceId(), span.getSpanId(), null, null ) ) );
        }

        return Optional.empty();
    }
}
