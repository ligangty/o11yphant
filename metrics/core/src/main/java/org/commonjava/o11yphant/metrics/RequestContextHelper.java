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
package org.commonjava.o11yphant.metrics;

import org.commonjava.cdi.util.weft.ThreadContext;

import static org.commonjava.o11yphant.metrics.RequestContextConstants.END_NANOS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.RAW_IO_WRITE_NANOS;

/**
 * The scope annotations (Thread, Header, MDC) tell where the constant is available/used. The static methods are used
 * to manage contextual state in both MDC and ThreadContext.
 */
public class RequestContextHelper
{
    public static void setContext( final String key, final Object value )
    {
        org.slf4j.MDC.put( key, String.valueOf( value ) );
        ThreadContext.getContext( true ).computeIfAbsent( key, k -> value );
    }

    public static <T> T getContext( final String key )
    {
        return getContext( key, null );
    }

    public static <T> T getContext( final String key, final T defaultValue )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            Object v = ctx.get( key );
            return v == null ? defaultValue : (T) v;
        }

        return defaultValue;
    }

    public static void clearContext( final String key )
    {
        org.slf4j.MDC.remove( key );

        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            ctx.remove( key );
        }
    }

    public static long getRequestEndNanos()
    {
        return getContext( END_NANOS, System.nanoTime() );
    }

    public static long getRawIoWriteNanos()
    {
        return getContext( RAW_IO_WRITE_NANOS, 0L );
    }

}
