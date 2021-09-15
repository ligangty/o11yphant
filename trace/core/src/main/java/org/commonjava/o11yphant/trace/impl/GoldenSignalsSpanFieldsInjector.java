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
package org.commonjava.o11yphant.trace.impl;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.GOLDEN_SIGNALS_FUNCTIONS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_NS;

@ApplicationScoped
public class GoldenSignalsSpanFieldsInjector
                implements SpanFieldsInjector
{
    private GoldenSignalsMetricSet goldenSignalsMetricSet;

    private final Logger logger = LoggerFactory.getLogger( getClass().getName());

    @Inject
    public GoldenSignalsSpanFieldsInjector( GoldenSignalsMetricSet goldenSignalsMetricSet )
    {
        this.goldenSignalsMetricSet = goldenSignalsMetricSet;
    }

    @Override
    public void decorateSpanAtClose( SpanAdapter span )
    {
        // NOTE This is awkward, but we get http_status and latency_ms from the DefaultHoneycombManager's field injection.
        // Those fields give us measurements, and the presence of the event gives us the data point we can aggregate for
        // load calculations.
        // The thing we really need from this is a single field that contains traffic classifiers, so we can group
        // the metrics / traces by traffic type.
        //
        // To avoid disrupting the collection of normal metrics from GoldenSignalsFilter, we'll just chop up the function
        // classifiers and push the tokens into a single set. Indy, for example, publishes things like:
        // content.metadata
        // content.metadata.maven
        //
        // ...for a single request. Those would result in the following traffic_type field:
        //
        // traffic_type = content,metadata,maven
        //
        // Using this, we can filter using a WHERE CONTAINS(..) substring match to look for content.metadata, and
        // aggregate maven + NPM metadata traffic, if necessary.
        //
        // Or, we can use a GROUP BY traffic_type and see the different traffic types broken down in a single graph.
        //
        Collection<String> functions = RequestContextHelper.getContext( GOLDEN_SIGNALS_FUNCTIONS );
        if ( functions == null || functions.isEmpty() )
        {
            logger.trace( "No Golden SLI functions detected. Skipping span-field injection." );
            return;
        }

        Set<String> classifierTokens = new LinkedHashSet<>();
        functions.forEach( function -> goldenSignalsMetricSet.function( function ).ifPresent( metric->{
            String[] parts = function.split( "\\." );
            for ( int i = 0; i < parts.length - 1; i++ )
            {
                classifierTokens.add( parts[i] );
            }
        } ) );

        logger.trace( "Golden SLI traffic classifiers: {}", classifierTokens );

//        Double latencyNanos = RequestContextHelper.getContext( REQUEST_LATENCY_NS );
        Double latencyMillis = RequestContextHelper.getContext( REQUEST_LATENCY_MILLIS  );
        if ( latencyMillis != null )
        {
            logger.trace( "Adding latency to span: {}", latencyMillis );
            span.addField( "latency_ms", latencyMillis );
        }
        logger.trace( "Adding traffic_type to span: {}", classifierTokens );
        span.addField( "traffic_type", StringUtils.join( classifierTokens, "," ) );
    }
}
