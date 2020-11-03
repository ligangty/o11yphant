package org.commonjava.o11yphant.metrics.jaxrs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.commonjava.o11yphant.metrics.conf.PrometheusConfig;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

public class PrometheusFilteringRegistry
                extends MetricRegistry
{
    private final MetricRegistry delegate;

    private final PrometheusConfig config;

    public PrometheusFilteringRegistry( MetricRegistry delegate, PrometheusConfig config )
    {
        this.delegate = delegate;
        this.config = config;
    }

    @Override
    public SortedSet<String> getNames()
    {
        return super.getNames();
    }

    @Override
    public SortedMap<String, Gauge> getGauges()
    {
        return filter(delegate.getGauges() );
    }

    @Override
    public SortedMap<String, Gauge> getGauges( MetricFilter filter )
    {
        return super.getGauges( filter );
    }

    @Override
    public SortedMap<String, Counter> getCounters()
    {
        return super.getCounters();
    }

    @Override
    public SortedMap<String, Counter> getCounters( MetricFilter filter )
    {
        return filter( delegate.getCounters( filter ) );
    }

    @Override
    public SortedMap<String, Histogram> getHistograms()
    {
        return filter( delegate.getHistograms() );
    }

    @Override
    public SortedMap<String, Histogram> getHistograms( MetricFilter filter )
    {
        return filter( delegate.getHistograms( filter ) );
    }

    @Override
    public SortedMap<String, Meter> getMeters()
    {
        return filter( delegate.getMeters() );
    }

    @Override
    public SortedMap<String, Meter> getMeters( MetricFilter filter )
    {
        return filter( delegate.getMeters( filter ) );
    }

    @Override
    public SortedMap<String, Timer> getTimers()
    {
        return filter( delegate.getTimers() );
    }

    @Override
    public SortedMap<String, Timer> getTimers( MetricFilter filter )
    {
        return filter( delegate.getTimers( filter ) );
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        return filter( delegate.getMetrics() );
    }

    private <T extends Metric> SortedMap<String, T> filter( Map<String, T> input )
    {
        TreeMap<String, T> result = new TreeMap<>();
        input.forEach( (k,v)->{
            if (config.isMetricExpressed(k)){
                result.put( k, v );
            }
        } );
        return result;
    }
}
