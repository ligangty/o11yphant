package org.commonjava.o11yphant.api;

import java.util.Map;

public interface MetricSet
{
    Map<String, Metric> getMetrics();
}
