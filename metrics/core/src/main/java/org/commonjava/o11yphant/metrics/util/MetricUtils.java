package org.commonjava.o11yphant.metrics.util;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.DefaultMetricRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class MetricUtils
{
    // for test
    public static DefaultMetricRegistry newDefaultMetricRegistry()
    {
        return new DefaultMetricRegistry( new com.codahale.metrics.MetricRegistry(), new HealthCheckRegistry() );
    }

    public static MetricSet wrapGaugeSet( Map<String, com.codahale.metrics.Metric> metrics )
    {
        return () -> metrics.entrySet()
                            .stream()
                            .collect( toMap( Map.Entry::getKey,
                                             e -> (Gauge<Object>) () -> ( (com.codahale.metrics.Gauge) e.getValue() ).getValue() ) );
    }

}
