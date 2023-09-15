/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
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
package org.commonjava.o11yphant.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.Histogram;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.metrics.impl.O11Histogram;
import org.commonjava.o11yphant.metrics.impl.O11Meter;
import org.commonjava.o11yphant.metrics.impl.O11Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricFilter.ALL;
import static org.apache.commons.lang3.StringUtils.join;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class DefaultMetricRegistry
                implements MetricRegistry
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * This only contains metric registered via {@link #register(String, Metric)} and {@link #register(String, MetricSet)}
     */
    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();

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
    public void clear()
    {
        logger.trace( "BEFORE CLEAR, found metrics:\n  {}", join( registry.getNames(), "\n  " ) );
        registry.removeMatching( ALL );
        logger.trace( "AFTER CLEAR, found metrics:\n  {}", join( registry.getNames(), "\n  " ) );
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
    public <T extends Metric> T register( String metricName, T metric )
    {
        logger.trace( "Registering: '{}'", metricName );
        if ( metric instanceof Gauge )
        {
            Gauge<?> gauge = (Gauge<?>) metric;
            registry.register( metricName, (com.codahale.metrics.Gauge<?>) gauge::getValue );
        }
        else if ( metric instanceof O11Meter )
        {
            registry.register( metricName, ( (O11Meter) metric ).getCodahaleMeter() );
        }
        else if ( metric instanceof O11Timer )
        {
            registry.register( metricName, ( (O11Timer) metric ).getCodahaleTimer() );
        }
        else if ( metric instanceof O11Histogram )
        {
            registry.register( metricName, ( (O11Histogram) metric ).getCodehaleHistogram() );
        }

        metrics.put( metricName, metric );
        return metric;
    }

    @Override
    public void register( String setName, MetricSet metricSet )
    {
        if ( metricSet != null )
        {
            logger.trace( "Registering metric-set named: {}", setName );
            metricSet.getMetrics().forEach( ( k, v ) -> {
                logger.trace( "Registering: '{}' in metric-set named: {}", k, setName );
                register( name( setName, k ), v );
            } );
        }
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
        registry.gauge( name, () -> o::getValue );
        return o;
    }

    @Override
    public Histogram histogram( String name )
    {
        return new O11Histogram( registry.histogram( name ) );
    }

    protected com.codahale.metrics.MetricRegistry getRegistry()
    {
        return registry;
    }

    // for test
    private boolean consoleReporterStarted;

    public void startConsoleReporter( int periodInSeconds )
    {
        if ( consoleReporterStarted )
        {
            return;
        }

        try (ConsoleReporter reporter = ConsoleReporter.forRegistry( registry )
                                                       .convertRatesTo( TimeUnit.SECONDS )
                                                       .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                       .build())
        {
            reporter.start( periodInSeconds, TimeUnit.SECONDS );
            consoleReporterStarted = true;
        }
    }
}
