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
package org.commonjava.o11yphant.trace.impl;

import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.jvm.JVMInstrumentation;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Retrieve values for various kinds of memory usage in the JVM, along with counts of various kinds of threads.
 */
@ApplicationScoped
public class JVMSpanFieldsInjector
                implements SpanFieldsInjector
{
    private final JVMInstrumentation jvmInstrumentation;

    @Inject
    public JVMSpanFieldsInjector( JVMInstrumentation jvmInstrumentation )
    {
        this.jvmInstrumentation = jvmInstrumentation;
    }

    /**
     * We will set some initial thread + memory measurements in the in-progress field set for the span, BUT
     * only if it's not already measured (if this is a child span, let's defer to the pre-existing starting measure).
     *
     * In the {@link #decorateSpanAtClose(SpanAdapter)} method, we'll grab these values, inject them as fields, and
     * also inject delta calculations as fields.
     */
    @Override
    public void decorateSpanAtStart( SpanAdapter span )
    {
        MetricSet memory = jvmInstrumentation.getMemoryUsageGaugeSet();
        if ( memory != null )
        {
            memory.getMetrics().forEach( ( k, v ) -> {
                String key = "jvm.start.memory." + k;
                // We don't want to override a pre-existing starting measurement...
                if ( shouldReportMemory( k ) && span.getInProgressField( key, null ) == null )
                {
                    span.setInProgressField( key, Double.valueOf( String.valueOf( ( (Gauge<?>) v ).getValue() ) ) );
                }
            } );
        }
        MetricSet threads = jvmInstrumentation.getThreadStatesGaugeSet();
        if ( threads != null )
        {
            threads.getMetrics().forEach( ( k, v ) -> {
                String key = "jvm.start.threads." + k;
                // We don't want to override a pre-existing starting measurement...
                if ( shouldReportThreads( k ) && span.getInProgressField( key, null ) == null )
                {
                    Double val = Double.valueOf( String.valueOf( ( (Gauge<?>) v ).getValue() ) );
                    span.setInProgressField( key, val );
                }
            } );
        }
    }

    /**
     * For each measurement type, grab starting measurements from in-progress fields, if we have them, and inject the
     * following fields:
     * <ul>
     *     <li>starting measurement</li>
     *     <li>ending measurement</li>
     *     <li>delta measurement</li>
     * </ul>
     *
     */
    @Override
    public void decorateSpanAtClose( SpanAdapter span )
    {
        MetricSet memory = jvmInstrumentation.getMemoryUsageGaugeSet();
        if ( memory != null )
        {
            memory.getMetrics().forEach( ( k, v ) -> {
                if ( shouldReportMemory( k ) )
                {
                    Double endValue = Double.valueOf( String.valueOf( ( (Gauge<?>) v ).getValue() ) );
                    span.addField( "jvm.end.memory." + k, endValue );

                    Double startValue = span.getInProgressField( "jvm.start.memory." + k, null );
                    if ( startValue != null )
                    {
                        span.clearInProgressField( "jvm.start.memory." + k );

                        span.addField( "jvm.start.memory." + k, startValue );
                        span.addField( "jvm.delta.memory." + k, endValue - startValue );
                    }
                }
            } );
        }
        MetricSet threads = jvmInstrumentation.getThreadStatesGaugeSet();
        if ( threads != null )
        {
            threads.getMetrics().forEach( ( k, v ) -> {
                if ( shouldReportThreads( k ) )
                {
                    @SuppressWarnings( "unchecked" )
                    long endValue = ( (Gauge<Number>) v ).getValue().longValue();
                    span.addField( "jvm.end.threads." + k, endValue );

                    Double startValue = span.getInProgressField( "jvm.start.threads." + k, null );
                    if ( startValue != null )
                    {
                        span.clearInProgressField( "jvm.start.threads." + k );

                        span.addField( "jvm.start.threads." + k, startValue );
                        span.addField( "jvm.delta.threads." + k, endValue - startValue );
                    }
                }
            } );
        }
    }

    /**
     * Filter memory gauges, rule out verbose pools.* keys.
     */
    private boolean shouldReportMemory( String k )
    {
        return !k.contains( "pool" );
    }

    /**
     * Filter threads gauges, rule out non-count keys.
     */
    private boolean shouldReportThreads( String k )
    {
        return k.endsWith( "count" );
    }
}
