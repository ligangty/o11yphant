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

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;
import io.opentelemetry.context.Scope;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.trace.thread.ThreadTracingContext;

import javax.annotation.Nonnull;
import java.util.Properties;

public class OtelThreadTracingContext
        implements ThreadTracingContext, ContextStorageProvider, ContextStorage
{
    private static final String OTEL_ATTACHED = "opentelemetry-attached-context";

    static
    {
        Properties properties = System.getProperties();
        properties.put("io.opentelemetry.context.contextStorageProvider", OtelThreadTracingContext.class);
    }
    @Override
    public void reinitThreadSpans()
    {
    }

    @Override
    public void clearThreadSpans()
    {
        // FIXME: Not sure this is meaningful at all...
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            ctx.remove( OTEL_ATTACHED );
        }
    }

    @Override
    public ContextStorage get()
    {
        return this;
    }

    @Override
    public Scope attach( @Nonnull Context context )
    {
        ThreadContext.getContext( true ).put( OTEL_ATTACHED, context );
        return () -> {
            ThreadContext ctx = ThreadContext.getContext( false );
            if ( ctx != null )
            {
                ctx.remove( OTEL_ATTACHED );
            }
        };
    }

    @Override
    public Context current()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            return (Context) ctx.get( OTEL_ATTACHED );
        }

        return Context.root();
    }
}
