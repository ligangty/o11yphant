package org.commonjava.o11yphant.trace.thread;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;

import java.util.Optional;

public interface ThreadTracingContext<T extends TracerType>
{
    void reinitThreadSpans();

    void clearThreadSpans();
}
