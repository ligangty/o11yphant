package org.commonjava.o11yphant.otel.impl;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpanContext;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

public class OtelContextPropagator implements ContextPropagator<OtelType>
{

    private OpenTelemetry otel;

    public OtelContextPropagator( OpenTelemetry otel )
    {
        this.otel = otel;
    }

    private TextMapGetter<HttpServletRequest> hsrGetter = new TextMapGetter<HttpServletRequest>()
    {
        @Override
        public Iterable<String> keys( HttpServletRequest carrier )
        {
            return toList(carrier.getHeaderNames());
        }

        @Override
        public String get( HttpServletRequest carrier, String key )
        {
            return carrier.getHeader( key );
        }
    };

    @Override
    public Optional<SpanContext<OtelType>> extractContext( HttpServletRequest request )
    {
        Context extracted =
                        otel.getPropagators().getTextMapPropagator().extract( Context.current(), request, hsrGetter );

        return Optional.of( new OtelSpanContext( extracted ) );
    }

    private static final List<String> toList( Enumeration<String> names )
    {
        ArrayList<String> result = new ArrayList<>();
        if ( names != null )
        {
            while ( names.hasMoreElements() )
            {
                result.add( names.nextElement() );
            }
        }
        return result;
    }
}
