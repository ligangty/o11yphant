package org.commonjava.o11yphant.trace.spi;

import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;

import java.util.Optional;

public interface SpanProvider<T extends TracerType>
{
    SpanAdapter<T> startServiceRootSpan( String spanName, Optional<SpanContext<T>> parentContext );

    SpanAdapter<T> startChildSpan( String spanName, Optional<SpanContext<T>> parentContext );

}
