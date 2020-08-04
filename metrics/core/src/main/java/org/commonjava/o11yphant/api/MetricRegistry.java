package org.commonjava.o11yphant.api;

public interface MetricRegistry
{
    <T extends Metric> T register( String name, T metric );

    void register( String name, MetricSet metricSet );

    void registerHealthCheck( String name, HealthCheck healthCheck );
}
