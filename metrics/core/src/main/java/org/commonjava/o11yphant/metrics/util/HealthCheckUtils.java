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
package org.commonjava.o11yphant.metrics.util;

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;

import java.util.Map;

public class HealthCheckUtils
{
    public static com.codahale.metrics.health.HealthCheck wrap( final HealthCheck hc )
    {
        return new com.codahale.metrics.health.HealthCheck()
        {
            @Override
            protected Result check() throws Exception
            {
                HealthCheck.Result result = hc.check();
                ResultBuilder builder = Result.builder();
                if ( result.isHealthy() )
                {
                    builder.healthy();
                }
                else
                {
                    builder.unhealthy();
                    builder.withMessage( result.getMessage() );
                    Map<String, Object> details = result.getDetails();
                    if ( details != null )
                    {
                        details.forEach( builder::withDetail );
                    }
                }
                return builder.build();
            }
        };
    }

}
