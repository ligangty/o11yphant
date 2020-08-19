package org.commonjava.o11yphant.metrics.util;

import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class MetricUtils
{
    public static MetricSet wrapGaugeSet( Map<String, com.codahale.metrics.Metric> metrics )
    {
        return () -> metrics.entrySet()
                            .stream()
                            .collect( toMap( Map.Entry::getKey,
                                             e -> (Gauge<Object>) () -> ( (com.codahale.metrics.Gauge) e.getValue() ).getValue() ) );
    }

}
