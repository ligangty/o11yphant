/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/o11yphant)
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
package org.commonjava.o11yphant.metrics.healthcheck.impl.component;

import org.commonjava.o11yphant.metrics.healthcheck.impl.HealthCheckResult;

import javax.inject.Named;

@Named
public class HeapHealthCheck
                extends ComponentHealthCheck
{
    private static final double GB = Math.pow( 1024, 3 );

    private static final String FREE_GB = "free-gb";

    private static final String USED_GB = "used-gb";

    private static final String TOTAL_GB = "total-gb";

    private static final String MAX_GB = "max-gb";

    private static final String CURRENT_LOAD = "current-load-pct";

    private static final float HEALTHY_LOAD_MAX = 90f;

    @Override
    public Result check() throws Exception
    {
        Runtime runtime = Runtime.getRuntime();

        double free = runtime.freeMemory();
        double total = runtime.totalMemory();
        double used = total - free;

        double max = runtime.maxMemory();
        double load = 100 * ( used / max );

        boolean isHealthy = true;
        if ( load > HEALTHY_LOAD_MAX )
        {
            isHealthy = false;
        }
        HealthCheckResult ret = new HealthCheckResult( isHealthy );

        ret.withDetail( FREE_GB, free / GB )
           .withDetail( USED_GB, used / GB )
           .withDetail( MAX_GB, max / GB )
           .withDetail( TOTAL_GB, total / GB )
           .withDetail( CURRENT_LOAD, load );

        return ret;
    }
}
