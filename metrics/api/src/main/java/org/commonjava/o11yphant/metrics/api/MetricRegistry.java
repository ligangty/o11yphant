package org.commonjava.o11yphant.metrics.api;

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;

import java.util.Map;

public interface MetricRegistry
{
    <T extends Metric> T register( String name, T metric );

    void register( String name, MetricSet metricSet );

    Map<String, Metric> getMetrics();

    void registerHealthCheck( String name, HealthCheck healthCheck );

    Meter meter( String name );

    Timer timer( String name );

    <T> Gauge<T> gauge( String name, Gauge<T> o );
}
