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
                        details.forEach( ( k, v ) -> builder.withDetail( k, v ) );
                    }
                }
                return builder.build();
            }
        };
    }

}
