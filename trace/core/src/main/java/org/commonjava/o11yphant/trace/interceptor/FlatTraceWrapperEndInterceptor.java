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

import org.commonjava.o11yphant.metrics.annotation.MetricWrapperEnd;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.util.InterceptorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;

@Interceptor
@MetricWrapperEnd
public class FlatTraceWrapperEndInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TracerConfiguration config;

    @SuppressWarnings( "rawtypes" )
    @Inject
    private TraceManager traceManager;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        String name = InterceptorUtils.getMetricNameFromContext( context );
        logger.trace( "START: trace metrics-end wrapper: {}", name );
        if ( !config.isEnabled() )
        {
            logger.trace( "SKIP: trace metrics-end wrapper: {}", name );
            return context.proceed();
        }

        if ( name == null || SKIP_METRIC.equals( name ) || config.getSampleRate( context.getMethod() ) < 1 )
        {
            logger.trace( "SKIP: trace metrics-end wrapper (span not configured: {})", name );
            return context.proceed();
        }

        Optional<SpanAdapter> span = TraceManager.getActiveSpan();
        try
        {
            span.ifPresent( s->traceManager.addEndField( s, name, currentTimeMillis() ) );
            return context.proceed();
        }
        finally
        {
            logger.trace( "END: trace metrics-end wrapper: {}", name );
        }
    }

}
