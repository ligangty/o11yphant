package org.commonjava.o11yphant.api;

public interface HealthCheck
{
    Result check() throws Exception;

    interface Result
    {
        boolean isHealthy();

        String getMessage();

        Throwable getError();

        String getTimestamp();
    }
}
