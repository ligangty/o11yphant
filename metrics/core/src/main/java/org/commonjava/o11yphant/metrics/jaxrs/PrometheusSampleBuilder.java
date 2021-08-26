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

import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class PrometheusSampleBuilder
        extends DefaultSampleBuilder
{
    private static final String LABEL_NODE = "node";

    private static final String LABEL_HOSTNAME = "hostname";

    private String nodeLabel;

    public PrometheusSampleBuilder( String nodeLabel )
    {
        super();
        this.nodeLabel = nodeLabel;
    }

    @Override
    public Collector.MetricFamilySamples.Sample createSample( final String dropwizardName, final String nameSuffix,
                                                              final List<String> additionalLabelNames,
                                                              final List<String> additionalLabelValues,
                                                              final double value )
    {
        List<String> labelNames = additionalLabelNames == null ? new ArrayList<>() : new ArrayList<>( additionalLabelNames );
        List<String> labelValues = additionalLabelValues == null ? new ArrayList<>() : new ArrayList<>( additionalLabelValues );

        if ( isNotEmpty( nodeLabel ) )
        {
            labelNames.add( LABEL_NODE );
            labelValues.add( nodeLabel );
        }

        String hostname = System.getenv( "HOSTNAME" );
        if ( isNotEmpty( hostname ) )
        {
            labelNames.add( LABEL_HOSTNAME );
            labelValues.add( hostname );
        }

        return super.createSample( dropwizardName, nameSuffix, labelNames, labelValues, value );
    }
}
