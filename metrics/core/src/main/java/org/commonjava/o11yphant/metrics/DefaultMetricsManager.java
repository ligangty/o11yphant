/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
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

import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapper;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperEnd;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperNamed;
import org.commonjava.o11yphant.metrics.annotation.MetricWrapperStart;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.api.healthcheck.CompoundHealthCheck;
import org.commonjava.o11yphant.metrics.healthcheck.impl.AbstractHealthCheck;
import org.commonjava.o11yphant.metrics.jvm.JVMInstrumentation;
import org.commonjava.o11yphant.metrics.util.NameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_COUNT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.CUMULATIVE_TIMINGS;
import static org.commonjava.o11yphant.metrics.MetricsConstants.DEFAULT;
import static org.commonjava.o11yphant.metrics.MetricsConstants.EXCEPTION;
import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.MetricsConstants.SKIP_METRIC;
import static org.commonjava.o11yphant.metrics.MetricsConstants.TIMER;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.IS_METERED;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getDefaultName;
import static org.commonjava.o11yphant.metrics.util.HealthCheckUtils.wrap;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class DefaultMetricsManager
                implements MetricsManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    private HealthCheckRegistry codahaleHealthCheckRegistry;

    @Inject
    private Instance<AbstractHealthCheck> healthChecks;

    @Inject
    private Instance<CompoundHealthCheck> compoundHealthChecks;

    @Inject
    private Instance<MetricSetProvider> metricSetProviderInstances;

    @Inject
    private JVMInstrumentation jvmInstrumentation;

    @Inject
    private MetricsConfig config;

    private Random random = new Random();

    public DefaultMetricsManager()
    {
    }

    // for test
    public DefaultMetricsManager( MetricRegistry metricRegistry )
    {
        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    public void init()
    {
        if ( !config.isEnabled() )
        {
            logger.info( "Metrics subsystem not enabled" );
            return;
        }

        logger.info( "Init metrics subsystem..." );

        jvmInstrumentation.registerJvmMetric( config.getNodePrefix() );

        // Health checks
        healthChecks.forEach( hc -> {
            logger.info( "Registering health check: {}", hc.getName() );
            codahaleHealthCheckRegistry.register( hc.getName(), wrap( hc ) );
        } );

        compoundHealthChecks.forEach( cc -> {
            Map<String, HealthCheck> healthChecks = cc.getHealthChecks();
            logger.info( "Registering {} health checks from: {}", healthChecks.size(), cc.getClass().getSimpleName() );
            healthChecks.forEach( ( name, check ) -> {
                logger.info( "Registering health check: {}", name );
                codahaleHealthCheckRegistry.register( name, wrap( check ) );
            } );
        } );

        metricSetProviderInstances.forEach( ( provider ) -> {
            if ( provider.isEnabled() )
            {
                metricRegistry.register( provider.getName(), provider.getMetricSet() );
            }
        } );
    }

    public boolean isMetered( Supplier<Boolean> meteringOverride )
    {
        int meterRatio = config.getMeterRatio();
        if ( meterRatio <= 1 || random.nextInt() % meterRatio == 0 )
        {
            return true;
        }
        else if ( meteringOverride != null && Boolean.TRUE.equals( meteringOverride.get() ) )
        {
            return true;
        }

        return false;
    }

    @MetricWrapperStart
    public Timer.Context startTimer( @MetricWrapperNamed String name )
    {
        return startTimerInternal( name );
    }

    private Timer.Context startTimerInternal( String name )
    {
        Timer.Context tctx = metricRegistry.timer( name ).time();
        ThreadContext ctx = ThreadContext.getContext( true );
        ctx.put( TIMER + name, tctx );
        return tctx;
    }

    @MetricWrapperEnd
    public long stopTimer( @MetricWrapperNamed String name )
    {
        return stopTimerInternal( name );
    }

    private long stopTimerInternal( String name )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx == null )
        {
            return 0;
        }

        Timer.Context tctx = (Timer.Context) ctx.get( TIMER + name );
        if ( tctx != null )
        {
            ctx.remove( TIMER + name );
            return tctx.stop();
        }

        return 0;
    }

    public Meter getMeter( String name )
    {
        return metricRegistry.meter( name );
    }

    public void accumulate( String name, final double elapsed )
    {
        ThreadContext ctx = ThreadContext.getContext( true );
        if ( ctx != null )
        {
            if ( !checkMetered( ctx ) )
            {
                return;
            }

            ctx.putIfAbsent( CUMULATIVE_TIMINGS, new ConcurrentHashMap<>() );
            Map<String, Double> timingMap = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );

            timingMap.merge( name, elapsed, ( existingVal, newVal ) -> existingVal + newVal );

            ctx.putIfAbsent( CUMULATIVE_COUNT, new ConcurrentHashMap<>() );
            Map<String, Integer> countMap = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNT );

            countMap.merge( name, 1, ( existingVal, newVal ) -> existingVal + 1 );
        }
    }

    @MetricWrapper
    public <T> T wrapWithStandardMetrics( final Supplier<T> method,
                                          @MetricWrapperNamed final Supplier<String> classifier )
    {
        String name = classifier.get();
        if ( !checkMetered() || SKIP_METRIC.equals( name ) )
        {
            return method.get();
        }

        String nodePrefix = config.getNodePrefix();

        String metricName = name( nodePrefix, name );
        String startName = name( metricName, "starts" );

        String timerName = name( metricName, TIMER );
        String errorName = name( name, EXCEPTION );
        String eClassName = null;

        Timer.Context timer = startTimerInternal( timerName );
        logger.trace( "START: {} ({})", metricName, timer );

        long start = System.nanoTime();
        try
        {
            mark( Arrays.asList( startName ) );

            return method.get();
        }
        catch ( Throwable e )
        {
            eClassName = name( name, EXCEPTION, e.getClass().getSimpleName() );
            mark( Arrays.asList( errorName, eClassName ) );

            throw e;
        }
        finally
        {
            stopTimers( Collections.singletonMap( timerName, timer ) );
            mark( Arrays.asList( metricName ) );

            double elapsed = ( System.nanoTime() - start ) / NANOS_PER_MILLISECOND;
            accumulate( metricName, elapsed );
        }
    }

    public boolean checkMetered()
    {
        return checkMetered( null );
    }

    public boolean checkMetered( ThreadContext ctx )
    {
        if ( ctx == null )
        {
            ctx = ThreadContext.getContext( false );
        }

        return ( ctx == null || ( (Boolean) ctx.getOrDefault( IS_METERED, Boolean.TRUE ) ) );
    }

    public void stopTimers( final Map<String, Timer.Context> timers )
    {
        if ( timers != null )
        {
            timers.forEach( ( name, timer ) -> stopTimerInternal( name ) );
        }
    }

    public void mark( final Collection<String> meters )
    {
        meters.forEach( name -> {
            getMeter( name ).mark();
        } );
    }

    public void addGauges( Class<?> className, String method, Map<String, Gauge<Integer>> gauges )
    {
        String defaultName = getDefaultName( className, method );
        gauges.forEach( ( k, v ) -> {
            String name = NameUtils.getName( config.getNodePrefix(), DEFAULT, defaultName, k );
            metricRegistry.gauge( name, v );
        } );
    }

    public MetricRegistry getMetricRegistry()
    {
        return metricRegistry;
    }

    public MetricsConfig getConfig()
    {
        return config;
    }

}
