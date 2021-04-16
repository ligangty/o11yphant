package org.commonjava.o11yphant.trace.spi;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface ContextPropagator<T extends TracerType>
{
    Optional<SpanContext<T>> extractContext( HttpRequest request );

    Optional<SpanContext<T>> extractContext( HttpServletRequest request );

    void injectContext( HttpUriRequest request, SpanAdapter clientSpan );

    void injectContext( HttpRequest request, SpanAdapter clientSpan );

    Optional<SpanContext<T>> extractContext( ThreadedTraceContext threadedContext );
}
