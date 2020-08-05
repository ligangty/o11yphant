package org.commonjava.o11yphant.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.api.Gauge;
import org.commonjava.o11yphant.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.api.Meter;
import org.commonjava.o11yphant.api.Metric;
import org.commonjava.o11yphant.api.MetricRegistry;
import org.commonjava.o11yphant.api.MetricSet;
import org.commonjava.o11yphant.api.Timer;
import org.commonjava.o11yphant.metrics.impl.O11Meter;
import org.commonjava.o11yphant.metrics.impl.O11Timer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class DefaultMetricRegistry
                implements MetricRegistry
{
    private final com.codahale.metrics.MetricRegistry registry;

    private final HealthCheckRegistry healthCheckRegistry;

    @Inject
    public DefaultMetricRegistry( com.codahale.metrics.MetricRegistry registry,
                                  HealthCheckRegistry healthCheckRegistry )
    {
        this.registry = registry;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    @Override
    public <T extends Metric> T register( String name, T metric )
    {
        if ( metric instanceof Gauge )
        {
            Gauge gauge = (Gauge) metric;
            registry.register( name, (com.codahale.metrics.Gauge) gauge::getValue );
        }
        return metric;
    }

    @Override
    public void register( String name, MetricSet metricSet )
    {
        metricSet.getMetrics().forEach( ( k, v ) -> register( name( name, k ), v ) );
    }

    @Override
    public void registerHealthCheck( String name, HealthCheck healthCheck )
    {
        healthCheckRegistry.register( name, new com.codahale.metrics.health.HealthCheck()
        {
            @Override
            protected Result check() throws Exception
            {
                HealthCheck.Result ret = healthCheck.check();
                ResultBuilder builder = Result.builder();
                if ( ret.isHealthy() )
                {
                    builder.healthy();
                }
                else
                {
                    builder.unhealthy( ret.getError() );
                    builder.withMessage( ret.getMessage() );
                }
                return builder.build();
            }
        } );
    }

    @Override
    public Meter meter( String name )
    {
        return new O11Meter( registry.meter( name ) );
    }

    @Override
    public Timer timer( String name )
    {
        return new O11Timer( registry.timer( name ) );
    }

    @Override
    public <T> Gauge<T> gauge( String name, Gauge<T> o )
    {
        registry.gauge( name, () -> () -> o.getValue() );
        return o;
    }
}
