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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpan;
import org.commonjava.o11yphant.otel.impl.adapter.OtelSpanContext;
import org.commonjava.o11yphant.otel.impl.adapter.OtelType;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class OtelContextPropagator
        implements ContextPropagator<OtelType>
{

    private final OpenTelemetry otel;

    public OtelContextPropagator( OpenTelemetry otel )
    {
        this.otel = otel;
    }

    private final TextMapGetter<Map<String, String>> mapGetter = new TextMapGetter<Map<String, String>>()
    {
        @Override
        public Iterable<String> keys( Map<String, String> carrier )
        {
            return carrier.keySet();
        }

        @Override
        @Nullable
        public String get( @Nullable Map<String, String> carrier, @Nullable String key )
        {
            return carrier == null || key == null ? null : carrier.get( key );
        }
    };

    @Override
    public Optional<SpanContext<OtelType>> extractContext( Supplier<Map<String, String>> headerSupplier )
    {
        Context extracted = otel.getPropagators()
                                .getTextMapPropagator()
                                .extract( Context.current(), headerSupplier.get(), mapGetter );

        return Optional.of( new OtelSpanContext( extracted ) );
    }

    @Override
    public void injectContext( BiConsumer<String, String> consumer, SpanAdapter clientSpan )
    {
        OtelSpan span = (OtelSpan) clientSpan;
        try (Scope scope = span.makeCurrent())
        {
            otel.getPropagators().getTextMapPropagator().inject( Context.current(), consumer, ( biConsumer, t, u ) -> {
                if ( biConsumer != null )
                {
                    biConsumer.accept( t, u );
                }
            } );
        }
    }

    @Override
    public Optional<SpanContext<OtelType>> extractContext( ThreadedTraceContext threadedContext )
    {
        return Optional.of( new OtelSpanContext( Context.current() ) );
    }

}
