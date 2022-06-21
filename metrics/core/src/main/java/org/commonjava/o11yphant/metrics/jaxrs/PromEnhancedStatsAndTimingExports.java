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
package org.commonjava.o11yphant.metrics.jaxrs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.commonjava.o11yphant.metrics.conf.PrometheusConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Modified copy of {@link DropwizardExports} that enables exporting mean, standard deviation, sample size in addition
 * to other metrics.
 */
public class PromEnhancedStatsAndTimingExports
                extends io.prometheus.client.Collector
                implements io.prometheus.client.Collector.Describable

{
    private final Logger logger = LoggerFactory.getLogger( getClass().getName() );

    private final PrometheusFilteringRegistry registry;

    private final PrometheusSampleBuilder sampleBuilder;

    public PromEnhancedStatsAndTimingExports( MetricRegistry metricRegistry, PrometheusConfig prometheusConfig )
    {
        this.registry = new PrometheusFilteringRegistry( metricRegistry, prometheusConfig );
        this.sampleBuilder = new PrometheusSampleBuilder( prometheusConfig.getNodeLabel() );
    }

    private static String getHelpMessage( String metricName )
    {
        return String.format( metricName );
    }

    /**
     * Export counter as Prometheus <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Gauge</a>.
     */
    MetricFamilySamples fromCounter( String dropwizardName, Counter counter )
    {
        MetricFamilySamples.Sample sample = sampleBuilder.createSample( dropwizardName, "", emptyList(), emptyList(),
                                                                        Long.valueOf( counter.getCount() )
                                                                            .doubleValue() );

        return new MetricFamilySamples( sample.name, Type.GAUGE, getHelpMessage( dropwizardName ),
                                        singletonList( sample ) );
    }

    /**
     * Export gauge as a prometheus gauge.
     */
    MetricFamilySamples fromGauge( String dropwizardName, Gauge<?> gauge )
    {
        Object obj = gauge.getValue();
        double value;
        if ( obj instanceof Number )
        {
            value = ( (Number) obj ).doubleValue();
        }
        else if ( obj instanceof Boolean )
        {
            value = ( (Boolean) obj ) ? 1 : 0;
        }
        else
        {
            logger.trace( String.format( "Invalid type for Gauge %s: %s", sanitizeMetricName( dropwizardName ),
                                         obj == null ? "null" : obj.getClass().getName() ) );
            return null;
        }
        MetricFamilySamples.Sample sample =
                        sampleBuilder.createSample( dropwizardName, "", emptyList(), emptyList(), value );
        return new MetricFamilySamples( sample.name, Type.GAUGE, getHelpMessage( dropwizardName ),
                                        singletonList( sample ) );
    }

    /**
     * Export a histogram snapshot as a prometheus SUMMARY.
     *
     * @param dropwizardName metric name.
     * @param snapshot       the histogram snapshot.
     * @param count          the total sample count for this snapshot.
     * @param factor         a factor to apply to histogram values.
     */
    MetricFamilySamples fromSnapshotAndCount( String dropwizardName, Snapshot snapshot, long count, double factor,
                                              String helpMessage, List<MetricFamilySamples.Sample> extraSamples )
    {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>();
        samples.add( sampleBuilder.createSample( dropwizardName, "", singletonList( "quantile" ), singletonList( "0.75" ),
                                                 snapshot.get75thPercentile() * factor ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "", singletonList( "quantile" ), singletonList( "0.95" ),
                                                 snapshot.get95thPercentile() * factor ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "", singletonList( "quantile" ), singletonList( "0.99" ),
                                                 snapshot.get99thPercentile() * factor ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "_raw_mean", emptyList(), emptyList(),
                                                 snapshot.getMean() ) );
        
        samples.add( sampleBuilder.createSample( dropwizardName, "_raw_stdev", emptyList(), emptyList(),
                                                 snapshot.getStdDev() ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "_count", emptyList(), emptyList(), count ) );

        samples.addAll( extraSamples );

        return new MetricFamilySamples( samples.get( 0 ).name, Type.SUMMARY, helpMessage, samples );
    }

    /**
     * Convert histogram snapshot.
     */
    MetricFamilySamples fromHistogram( String dropwizardName, Histogram histogram )
    {
        return fromSnapshotAndCount( dropwizardName, histogram.getSnapshot(), histogram.getCount(), 1.0,
                                     getHelpMessage( dropwizardName ), emptyList() );
    }

    /**
     * Export Dropwizard Timer as a histogram. Use TIME_UNIT as time unit.
     */
    MetricFamilySamples fromTimer( String dropwizardName, Timer timer )
    {
        return fromSnapshotAndCount( dropwizardName, timer.getSnapshot(), timer.getCount(),
                                     1.0D / TimeUnit.SECONDS.toNanos( 1L ), getHelpMessage( dropwizardName ),
                                     timerSamples( dropwizardName, timer ) );
    }

    private List<MetricFamilySamples.Sample> timerSamples( String dropwizardName, Timer timer )
    {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>();

        samples.add( sampleBuilder.createSample( dropwizardName, "_rate", singletonList( "mins" ), singletonList( "1" ),
                                                 timer.getOneMinuteRate() ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "_rate", singletonList( "mins" ), singletonList( "5" ),
                                                 timer.getFiveMinuteRate() ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "_rate", singletonList( "mins" ), singletonList( "15" ),
                                                 timer.getFifteenMinuteRate() ) );

        samples.add( sampleBuilder.createSample( dropwizardName, "_mean_rate", emptyList(), emptyList(),
                                                 timer.getMeanRate() ) );

        return samples;
    }

    /**
     * Export a Meter as as prometheus COUNTER.
     */
    MetricFamilySamples fromMeter( String dropwizardName, Meter meter )
    {
        final MetricFamilySamples.Sample sample =
                        sampleBuilder.createSample( dropwizardName, "_total", emptyList(), emptyList(),
                                                    meter.getCount() );
        return new MetricFamilySamples( sample.name, Type.COUNTER, getHelpMessage( dropwizardName ),
                                        singletonList( sample ) );
    }

    @Override
    public List<MetricFamilySamples> collect()
    {
        Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<>();

        for ( SortedMap.Entry<String, Gauge> entry : registry.getGauges().entrySet() )
        {
            addToMap( mfSamplesMap, fromGauge( entry.getKey(), entry.getValue() ) );
        }
        for ( SortedMap.Entry<String, Counter> entry : registry.getCounters().entrySet() )
        {
            addToMap( mfSamplesMap, fromCounter( entry.getKey(), entry.getValue() ) );
        }
        for ( SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet() )
        {
            addToMap( mfSamplesMap, fromHistogram( entry.getKey(), entry.getValue() ) );
        }
        for ( SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet() )
        {
            addToMap( mfSamplesMap, fromTimer( entry.getKey(), entry.getValue() ) );
        }
        for ( SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet() )
        {
            addToMap( mfSamplesMap, fromMeter( entry.getKey(), entry.getValue() ) );
        }
        return new ArrayList<>( mfSamplesMap.values() );
    }

    private void addToMap( Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples )
    {
        if ( newMfSamples != null )
        {
            MetricFamilySamples currentMfSamples = mfSamplesMap.get( newMfSamples.name );
            if ( currentMfSamples == null )
            {
                mfSamplesMap.put( newMfSamples.name, newMfSamples );
            }
            else
            {
                List<MetricFamilySamples.Sample> samples =
                                new ArrayList<>( currentMfSamples.samples );
                samples.addAll( newMfSamples.samples );
                mfSamplesMap.put( newMfSamples.name, new MetricFamilySamples( newMfSamples.name, currentMfSamples.type,
                                                                              currentMfSamples.help, samples ) );
            }
        }
    }

    @Override
    public List<MetricFamilySamples> describe()
    {
        return emptyList();
    }
}
