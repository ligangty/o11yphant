package org.commonjava.o11yphant.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.impl.O11Meter;
import org.commonjava.o11yphant.metrics.impl.O11Timer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class DefaultMetricRegistry
                implements MetricRegistry
{
    /**
     * This only contains metric registered via {@link #register(String, Metric)} and {@link #register(String, MetricSet)}
     */
    private final Map<String, Metric> metrics = new ConcurrentHashMap();

    private final com.codahale.metrics.MetricRegistry registry;

    private final HealthCheckRegistry healthCheckRegistry;

    @Inject
    public DefaultMetricRegistry( com.codahale.metrics.MetricRegistry registry,
                                  HealthCheckRegistry healthCheckRegistry )
    {
        this.registry = registry;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    /**
     * Register a detached / standalone metric.
     *
     * By using the {@link #gauge(String, Gauge)}, {@link #meter(String)}, or {@link #timer(String)}, the metrics are
     * registered via underlying codahale registry. We do not keep tracking them in this class. If user constructs
     * detached O11 metric objects, they can use this method to register and get them via {@link #getMetrics()}.
     * Those detached metrics are registered to underlying codahale registry as well.
     */
    @Override
    public <T extends Metric> T register( String name, T metric )
    {
        if ( metric instanceof Gauge )
        {
            Gauge gauge = (Gauge) metric;
            registry.register( name, (com.codahale.metrics.Gauge) gauge::getValue );
        }
        else if ( metric instanceof O11Meter )
        {
            registry.register( name, ( (O11Meter) metric ).getCodahaleMeter() );
        }
        else if ( metric instanceof O11Timer )
        {
            registry.register( name, ( (O11Timer) metric ).getCodahaleTimer() );
        }
        metrics.put( name, metric );
        return metric;
    }

    @Override
    public void register( String name, MetricSet metricSet )
    {
        metricSet.getMetrics().forEach( ( k, v ) -> register( name( name, k ), v ) );
    }

    /**
     * This only returns metric registered via {@link #register(String, Metric)} and {@link #register(String, MetricSet)}
     */
    @Override
    public Map<String, Metric> getMetrics()
    {
        return Collections.unmodifiableMap( metrics );
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

    protected com.codahale.metrics.MetricRegistry getRegistry()
    {
        return registry;
    }
}
