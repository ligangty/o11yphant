package org.commonjava.o11yphant.trace.spi;

import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface ContextPropagator<T extends TracerType>
{
    Optional<SpanContext<T>> extractContext( HttpServletRequest request );
}
