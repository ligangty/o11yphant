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
package org.commonjava.o11yphant.metrics.system;

import org.commonjava.o11yphant.metrics.api.MetricSet;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.MetricSetProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class SystemGaugesSetProvider
        implements MetricSetProvider
{
    @Inject
    private SystemGaugesSet systemGaugesSet;

    @Inject
    private MetricsConfig metricsConfig;

    private static final String SYSTEM = "system";

    @Override
    public MetricSet getMetricSet()
    {
        return systemGaugesSet;
    }

    @Override
    public String getName()
    {
        return name( metricsConfig.getNodePrefix(), SYSTEM );
    }

    @Override
    public void reset()
    {
        systemGaugesSet.reset();
    }

}
