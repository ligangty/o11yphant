package org.commonjava.o11yphant.trace.spi;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ContextPropagator<T extends TracerType>
{
    Optional<SpanContext<T>> extractContext( Supplier<Map<String, String>> headerSupplier );

    void injectContext( BiConsumer<String, String> injectorFunction, SpanAdapter spanAdapter );

    Optional<SpanContext<T>> extractContext( ThreadedTraceContext threadedContext );
}
