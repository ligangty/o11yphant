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
package org.commonjava.o11yphant.metrics.api;

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;

import java.util.Map;
import java.util.SortedSet;

public interface MetricRegistry
{
    void clear();

    <T extends Metric> T register( String name, T metric );

    void register( String name, MetricSet metricSet );

    Map<String, Metric> getMetrics();

    void registerHealthCheck( String name, HealthCheck healthCheck );

    Meter meter( String name );

    Timer timer( String name );

    <T> Gauge<T> gauge( String name, Gauge<T> o );

    Histogram histogram( String name );
}
