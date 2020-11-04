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
