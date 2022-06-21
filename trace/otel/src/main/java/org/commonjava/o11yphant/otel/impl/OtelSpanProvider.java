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
package org.commonjava.o11yphant.otel.impl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpan;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpanContext;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;

import java.util.Optional;

public class OtelSpanProvider implements SpanProvider<OtelType>
{

//    private OpenTelemetry otel;

    private Tracer tracer;

    @SuppressWarnings("PMD")
    public OtelSpanProvider( OpenTelemetrySdk otel, Tracer tracer )
    {
//        this.otel = otel;
        this.tracer = tracer;
    }

    @Override
    public SpanAdapter startServiceRootSpan( String spanName, Optional<SpanContext<OtelType>> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        if ( parentContext.isPresent() )
        {
            Context ctx = (( OtelSpanContext ) parentContext.get()).getContext();
            spanBuilder.setParent( ctx);
        }
        else
        {
            spanBuilder.setNoParent();
        }

        Span span = spanBuilder.setSpanKind( SpanKind.SERVER ).startSpan();
        span.makeCurrent();
        return new OtelSpan( span, true );
    }

    @Override
    public SpanAdapter startChildSpan( String spanName, Optional<SpanContext<OtelType>> parentContext )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        boolean isRoot = true;
        if ( parentContext.isPresent() )
        {
            Context ctx = (( OtelSpanContext ) parentContext.get()).getContext();
            spanBuilder.setParent( ctx);
            isRoot = false;
        }
        else
        {
            Context ctx = Context.current();
            if ( ctx != null )
            {
                spanBuilder.setParent( ctx );
                isRoot = false;
            }
            else
            {
                spanBuilder.setNoParent();
            }
        }

        Span span = spanBuilder.setSpanKind( SpanKind.INTERNAL ).startSpan();
        span.makeCurrent();
        return new OtelSpan( span, isRoot );
    }

    @Override
    public SpanAdapter startClientSpan( String spanName )
    {
        SpanBuilder spanBuilder = tracer.spanBuilder( spanName );
        Context ctx = Context.current();
        boolean localRoot = true;
        if ( ctx != null )
        {
            spanBuilder.setParent( ctx );
            localRoot = false;
        }
        else
        {
            spanBuilder.setNoParent();
        }

        Span span = spanBuilder.setSpanKind( SpanKind.CLIENT ).startSpan();
        span.makeCurrent();
        return new OtelSpan( span, localRoot );
    }
}
