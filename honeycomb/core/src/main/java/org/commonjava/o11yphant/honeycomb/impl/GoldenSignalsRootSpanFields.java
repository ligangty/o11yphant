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
package org.commonjava.o11yphant.honeycomb.impl;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.Metric;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class GoldenSignalsRootSpanFields
                implements RootSpanFields
{
    private GoldenSignalsMetricSet goldenSignalsMetricSet;

    @Inject
    public GoldenSignalsRootSpanFields( GoldenSignalsMetricSet goldenSignalsMetricSet )
    {
        this.goldenSignalsMetricSet = goldenSignalsMetricSet;
    }

    @Override
    public Map<String, Object> get()
    {
        final Map<String, Object> ret = new HashMap<>();

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
        final Map<String, Metric> metrics = goldenSignalsMetricSet.getMetrics();
        Set<String> classifierTokens = new LinkedHashSet<>();
        metrics.forEach( ( k, v ) -> {
            String[] parts = k.split( "\\." );
            for ( int i = 0; i < parts.length - 1; i++ )
            {
                classifierTokens.add( parts[i] );
            }
        } );

        ret.put( "traffic_type", StringUtils.join( classifierTokens, "," ) );
        return ret;
    }
}
