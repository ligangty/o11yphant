package org.commonjava.o11yphant.honeycomb.impl;

import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.jvm.JVMInstrumentation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieve values for various kinds of memory usage in the JVM, along with counts of various kinds of threads.
 */
@ApplicationScoped
public class JVMRootSpanFields
                implements RootSpanFields
{
    private JVMInstrumentation jvmInstrumentation;

    @Inject
    public JVMRootSpanFields( JVMInstrumentation jvmInstrumentation )
    {
        this.jvmInstrumentation = jvmInstrumentation;
    }

    @Override
    public Map<String, Object> get()
    {
        Map<String, Object> ret = new HashMap<>();
        MetricSet memory = jvmInstrumentation.getMemoryUsageGaugeSet();
        if ( memory != null )
        {
            memory.getMetrics().forEach( ( k, v ) -> {
                if ( shouldReportMemory( k ) )
                {
                    ret.put( "jvm.memory." + k, ( (Gauge) v ).getValue() );
                }
            } );
        }
        MetricSet threads = jvmInstrumentation.getThreadStatesGaugeSet();
        if ( threads != null )
        {
            threads.getMetrics().forEach( ( k, v ) -> {
                if ( shouldReportThreads( k ) )
                {
                    ret.put( "jvm.threads." + k, ( (Gauge) v ).getValue() );
                }
            } );
        }
        return ret;
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
