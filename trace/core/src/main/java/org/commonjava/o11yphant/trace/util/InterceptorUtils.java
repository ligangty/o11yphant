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
package org.commonjava.o11yphant.trace.util;

import org.commonjava.o11yphant.metrics.annotation.MetricWrapper;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperNamed;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperNamedAfterRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Supplier;

public class InterceptorUtils
{
    private static Logger logger = LoggerFactory.getLogger( InterceptorUtils.class );

    public static String getMetricNameFromContext( InvocationContext context )
    {
        return getMetricNameFromContextInternal( context, MetricWrapperNamed.class );
    }

    public static String getMetricNameFromContextAfterRun( InvocationContext context )
    {
        return getMetricNameFromContextInternal( context, MetricWrapperNamedAfterRun.class );
    }

    private static String getMetricNameFromContextInternal( InvocationContext context, Class annotationClass )
    {
        String name = null;
        Method method = context.getMethod();
        Parameter[] parameters = method.getParameters();
        for ( int i = 0; i < parameters.length; i++ )
        {
            Parameter param = parameters[i];
            Annotation annotation = param.getAnnotation( annotationClass );
            if ( annotation != null )
            {
                Object pv = context.getParameters()[i];
                if ( pv instanceof Supplier )
                {
                    name = String.valueOf( ( (Supplier) pv ).get() );
                }
                else
                {
                    name = String.valueOf( pv );
                }

                break;
            }
        }
        logger.debug( "Found metric name: {}, annotation: {}", name, annotationClass.getSimpleName() );
        return name;
    }

    /**
     * Wrap method with standard {@link MetricWrapper}. The FlatTraceWrapperInterceptor will pick it up
     * to add cumulative field. The field name is a combination of classifier which is the full name, e.g, with nodeId prefix,
     * plus the appendix which is calculated after the execution of the method.
     * The appendix gives caller a chance to append additional token to the name, e.g., the HTTP response code, etc.
     */
    @MetricWrapper
    public <T> T withStandardMetricWrapper( final Supplier<T> method,
                                            @MetricWrapperNamed final Supplier<String> classifier,
                                            @MetricWrapperNamedAfterRun final Supplier<String> appendix )
    {
        return method.get();
    }

}
