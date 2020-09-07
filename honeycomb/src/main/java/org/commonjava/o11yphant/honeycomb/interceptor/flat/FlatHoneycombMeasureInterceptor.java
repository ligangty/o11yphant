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
package org.commonjava.o11yphant.honeycomb.interceptor.flat;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.commonjava.o11yphant.honeycomb.HoneycombManager;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

import static java.lang.System.currentTimeMillis;

@Interceptor
@Measure
public class FlatHoneycombMeasureInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private HoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        Method method = context.getMethod();
        String name = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        logger.trace( "START: Honeycomb method wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: Honeycomb method wrapper: {}", name );
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
            logger.trace( "SKIP: Honeycomb method wrapper (no annotation or span is not configured: {})", name );
            return context.proceed();
        }

        long begin = currentTimeMillis();
        Span span = null;
        try
        {
            span = honeycombManager.getActiveSpan();
            return context.proceed();
        }
        finally
        {
            if ( span != null )
            {
                long elapse = currentTimeMillis() - begin;
                honeycombManager.addCumulativeField( span, name, elapse );
            }
            logger.trace( "END: Honeycomb method wrapper: {}", name );
        }
    }

}
