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
package org.commonjava.o11yphant.metrics.healthcheck.impl;

import org.commonjava.o11yphant.metrics.api.healthcheck.CompoundHealthCheck;
import org.commonjava.o11yphant.metrics.healthcheck.impl.component.ComponentHealthCheck;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.atomic.AtomicInteger;

@Named
public class SummaryHealthCheck
                extends AbstractHealthCheck
{
    enum SummaryRating
    {
        green, yellow, red
    }

    private static final String RATING = "rating";

    private static final String UNHEALTHY_COUNT = "unhealthy-count";

    @Inject
    private Instance<ComponentHealthCheck> looseComponents;

    @Inject
    private Instance<CompoundHealthCheck> looseCompounds;

    @Override
    public Result check()
    {
        AtomicInteger count = new AtomicInteger( 0 );
        looseComponents.forEach( check -> {
            if ( !check.execute().isHealthy() )
                count.incrementAndGet();
        } );

        looseCompounds.forEach( lc -> lc.getHealthChecks().forEach( ( k, check ) -> {
            try
            {
                if ( !check.check().isHealthy() )
                {
                    count.incrementAndGet();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        } ) );

        boolean isHealthy = count.get() <= 0;

        HealthCheckResult ret = new HealthCheckResult( isHealthy );
        if ( count.get() > 3 )
        {
            ret.withDetail( RATING, SummaryRating.red );
        }
        else if ( count.get() > 0 )
        {
            ret.withDetail( RATING, SummaryRating.yellow );
        }

        ret.withDetail( UNHEALTHY_COUNT, count.get() );

        return ret;
    }
}
