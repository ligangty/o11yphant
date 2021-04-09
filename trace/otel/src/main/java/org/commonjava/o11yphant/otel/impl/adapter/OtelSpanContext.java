package org.commonjava.o11yphant.otel.impl.adapter;

import io.opentelemetry.context.Context;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;

public class OtelSpanContext implements SpanContext<OtelType>
{
    private Context ctx;

    public OtelSpanContext( Context ctx )
    {
        this.ctx = ctx;
    }

    public Context getContext()
    {
        return ctx;
    }
}
