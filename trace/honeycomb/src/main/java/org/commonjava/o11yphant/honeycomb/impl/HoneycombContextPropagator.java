package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import io.honeycomb.beeline.tracing.propagation.W3CPropagationCodec;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombSpanContext;
import org.commonjava.o11yphant.honeycomb.impl.adapter.HoneycombType;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class HoneycombContextPropagator implements ContextPropagator<HoneycombType>
{
    @Override
    public Optional<SpanContext<HoneycombType>> extractContext( HttpServletRequest request )
    {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while ( headerNames.hasMoreElements() )
        {
            String key = headerNames.nextElement();
            headers.put( key, request.getHeader( key ) );
        }

        PropagationContext context = W3CPropagationCodec.getInstance().decode( headers );

        return context.isTraced() ?
                        Optional.of( new HoneycombSpanContext(
                                        new PropagationContext( context.getTraceId(), context.getSpanId(), null,
                                                                null ) ) ) :
                        Optional.empty();
    }
}
