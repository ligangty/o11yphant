/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.o11yphant.honeycomb;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.honeycomb.beeline.tracing.Span;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class SpanContext
{
    private final String traceId;

    private final String parentSpanId;

    private final String spanId;

    private Map<String, Timer> timerMap = new HashMap<>();

    private Map<String, Meter> meterMap = new HashMap<>();

    public SpanContext( final String traceId, final String spanId, final String parentSpanId )
    {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    public SpanContext( final Span span )
    {
        this.traceId = span.getTraceId();
        this.spanId = span.getSpanId();
        this.parentSpanId = span.getParentSpanId();
    }

    public String getParentSpanId()
    {
        return parentSpanId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public String getSpanId()
    {
        return spanId;
    }

    @Override
    public String toString()
    {
        return "SpanContext{" + "traceId='" + traceId + '\'' + ", spanId='" + spanId + '\'' + ", parentSpanId='"
                        + parentSpanId + '\'' + '}';
    }

    public void putTimer( String timerName, Timer timer )
    {
        timerMap.put( timerName, timer );
    }

    public Timer getTimer( String timerName )
    {
        return timerMap.get( timerName );
    }

    public Meter getMeter( String meterName )
    {
        return meterMap.get( meterName );
    }

    public void putMeter( String meterName, Meter meter )
    {
        meterMap.put( meterName, meter );
    }

    public Map<String, Timer> getTimers()
    {
        return unmodifiableMap( timerMap );
    }

    public Map<String, Meter> getMeters()
    {
        return unmodifiableMap( meterMap );
    }
}
