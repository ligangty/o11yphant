/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
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
package org.commonjava.o11yphant.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.commonjava.o11yphant.otel.impl.OtelContextPropagator;
import org.commonjava.o11yphant.otel.impl.OtelSpanProvider;
import org.commonjava.o11yphant.otel.impl.OtelThreadTracingContext;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.ContextPropagator;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanProvider;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;

public class OtelTracePlugin
        implements O11yphantTracePlugin
{
    private OtelThreadTracingContext threadTracingContext;

    private OtelContextPropagator contextPropagator;

    private OtelSpanProvider spanProvider;

    public OtelTracePlugin( TracerConfiguration traceConfiguration, OtelConfiguration otelConfig,
                            SpanExporter... exporters )
    {
        if ( traceConfiguration.isEnabled() )
        {
            OpenTelemetry otel = OtelUtil.getOpenTelemetry( traceConfiguration, otelConfig, exporters );
            Tracer tracer =
                    otel.getTracer( otelConfig.getInstrumentationName(), otelConfig.getInstrumentationVersion() );

            this.contextPropagator = new OtelContextPropagator( otel );
            this.spanProvider = new OtelSpanProvider( tracer );
            this.threadTracingContext = new OtelThreadTracingContext();
        }
    }

    @Override
    public SpanProvider getSpanProvider()
    {
        return spanProvider;
    }

    @Override
    public ContextPropagator getContextPropagator()
    {
        return contextPropagator;
    }

    @Override
    public ThreadTracingContext getThreadTracingContext()
    {
        return threadTracingContext;
    }

}
