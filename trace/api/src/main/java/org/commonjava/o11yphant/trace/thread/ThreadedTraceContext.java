package org.commonjava.o11yphant.trace.thread;

import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.spi.adapter.TracerType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ThreadedTraceContext
{
    private Optional<SpanAdapter> activeSpan;

    private final Map<String, Timer> timers = new HashMap<>();

    private Map<String, Meter> meters = new HashMap<>();

    private SpanContext<? extends TracerType> spanCtx;

    public ThreadedTraceContext( Optional<SpanAdapter> activeSpan )
    {
        this.activeSpan = activeSpan;
    }

    public Map<String, Timer> getTimers()
    {
        return timers;
    }

    public Map<String, Meter> getMeters()
    {
        return meters;
    }

    public Optional<SpanAdapter> getActiveSpan()
    {
        return activeSpan;
    }
}
