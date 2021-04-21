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
package org.commonjava.o11yphant.trace.interceptor;

import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Interceptor
@Measure
public class FlatTraceMeasureInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TracerConfiguration config;

    @Inject
    private TraceManager traceManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        Method method = context.getMethod();
        String name = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        logger.trace( "START: trace method wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: trace method wrapper: {}", name );
            return context.proceed();
        }

        Measure measure = method.getAnnotation( Measure.class );
        if ( measure == null )
        {
            measure = method.getDeclaringClass().getAnnotation( Measure.class );
        }

        int sampleRate = config.getSampleRate( name );
        if ( measure == null || sampleRate < 1 )
        {
            logger.trace( "SKIP: trace method wrapper (no annotation or span is not configured: {})", name );
            return context.proceed();
        }

        long begin = currentTimeMillis();
        try
        {
            return context.proceed();
        }
        finally
        {
            Optional<SpanAdapter> span = traceManager.getActiveSpan();
            span.ifPresent( s->{
                long elapse = currentTimeMillis() - begin;
                traceManager.addCumulativeField( s, name, elapse );
            } );
            logger.trace( "END: trace method wrapper: {}", name );
        }
    }

}
