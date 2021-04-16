package org.commonjava.o11yphant.trace.spi;

import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;

public interface O11yphantTracePlugin<T extends TracerType>
{
    SpanProvider<T> getSpanProvider();

    ContextPropagator<T> getContextPropagator();

    ThreadTracingContext<T> getThreadTracingContext();
}
