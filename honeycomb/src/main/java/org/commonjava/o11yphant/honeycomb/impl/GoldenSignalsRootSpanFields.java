package org.commonjava.o11yphant.honeycomb.impl;

import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public abstract class GoldenSignalsRootSpanFields
                implements RootSpanFields
{
    private GoldenSignalsMetricSet goldenSignalsMetricSet;

    @Inject
    public GoldenSignalsRootSpanFields( GoldenSignalsMetricSet goldenSignalsMetricSet )
    {
        this.goldenSignalsMetricSet = goldenSignalsMetricSet;
    }

    @Override
    public Map<String, Object> get()
    {
        final Map<String, Object> ret = new HashMap<>();

        final Map<String, Metric> metrics = goldenSignalsMetricSet.getMetrics();
        metrics.forEach( ( k, v ) -> {
            Object value = null;
            if ( v instanceof Gauge )
            {
                value = ( (Gauge) v ).getValue();
            }
            else if ( v instanceof Timer )
            {
                value = ( (Timer) v ).getSnapshot().get95thPercentile();
            }
            else if ( v instanceof Meter )
            {
                value = ( (Meter) v ).getOneMinuteRate();
            }
            ret.put( k, value );
        } );
        return ret;
    }
}
