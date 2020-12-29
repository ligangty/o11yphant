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
package org.commonjava.o11yphant.honeycomb.interceptor.vertical;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperStart;
import org.commonjava.o11yphant.honeycomb.DefaultHoneycombManager;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import static org.commonjava.o11yphant.honeycomb.util.InterceptorUtils.getMetricNameFromContext;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.getContext;

@Interceptor
@MetricWrapperStart
public class HoneycombWrapperStartInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HoneycombConfiguration config;

    @Inject
    private DefaultHoneycombManager honeycombManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        String name = getMetricNameFromContext( context );
        logger.trace( "START: Honeycomb metrics-start wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: Honeycomb metrics-start wrapper: {}", name );
            return context.proceed();
        }

        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( context.getMethod() ) < 1 )
        {
            logger.trace( "SKIP: Honeycomb metrics-start wrapper (no span name or span not configured: {})", name );
            return context.proceed();
        }

//        ThreadContext.getContext( true ).put( SAMPLE_OVERRIDE, Boolean.TRUE );
        try
        {
            Span span = honeycombManager.startChildSpan( name );
            logger.trace( "startChildSpan, span: {}, defaultName: {}", span, name );
        }
        catch ( Exception e )
        {
            logger.error( "Error in honeycomb subsystem! " + e.getMessage(), e );
        }
        finally
        {
            logger.trace( "END: Honeycomb metrics-start wrapper: {}", name );
        }

        return context.proceed();
    }

}
