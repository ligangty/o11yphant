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
package org.commonjava.o11yphant.honeycomb.impl.adapter;

import io.honeycomb.beeline.tracing.propagation.PropagationContext;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;

public class HoneycombSpanContext implements SpanContext<HoneycombType>
{
    private PropagationContext ctx;

    public HoneycombSpanContext( PropagationContext ctx )
    {
        this.ctx = ctx;
    }

    public PropagationContext getPropagationContext()
    {
        return ctx;
    }

    @Override
    public String toString()
    {
        return String.format( "Honeycomb trace context {trace: %s, parent-span: %s}", ctx.getTraceId(),
                              ctx.getSpanId() );
    }
}
