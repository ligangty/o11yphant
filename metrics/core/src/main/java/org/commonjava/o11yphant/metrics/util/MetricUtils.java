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
package org.commonjava.o11yphant.metrics.util;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.o11yphant.metrics.DefaultMetricRegistry;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class MetricUtils
{
    // for test
    public static DefaultMetricRegistry newDefaultMetricRegistry()
    {
        return new DefaultMetricRegistry( new com.codahale.metrics.MetricRegistry(), new HealthCheckRegistry() );
    }

    public static MetricSet wrapGaugeSet( Map<String, com.codahale.metrics.Metric> metrics )
    {
        return () -> metrics.entrySet()
                            .stream()
                            .collect( toMap( Map.Entry::getKey,
                                             e -> (Gauge<Object>) () -> ( (com.codahale.metrics.Gauge) e.getValue() ).getValue() ) );
    }

}
