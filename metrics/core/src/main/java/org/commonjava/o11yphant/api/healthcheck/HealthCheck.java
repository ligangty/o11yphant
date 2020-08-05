package org.commonjava.o11yphant.api.healthcheck;

import java.util.Map;

public interface HealthCheck
{
    Result check() throws Exception;

    interface Result
    {
        boolean isHealthy();

        String getMessage();

        Throwable getError();

        String getTimestamp();

        Map<String, Object> getDetails();
    }
}
