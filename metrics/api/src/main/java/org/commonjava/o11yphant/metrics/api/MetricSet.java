package org.commonjava.o11yphant.metrics.api;

import java.util.Map;

public interface MetricSet
{
    Map<String, Metric> getMetrics();
}
