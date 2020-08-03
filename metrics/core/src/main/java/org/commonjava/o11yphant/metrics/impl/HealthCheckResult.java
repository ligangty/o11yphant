package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.api.HealthCheck;

import java.util.Date;

public class HealthCheckResult
                implements HealthCheck.Result
{
    private boolean healthy;

    private String message;

    private Throwable err;

    public HealthCheckResult( boolean healthy )
    {
        this.healthy = healthy;
    }

    public HealthCheckResult withMessage( String message )
    {
        this.message = message;
        return this;
    }

    public HealthCheckResult withThrowable( Throwable err )
    {
        this.err = err;
        return this;
    }

    public static HealthCheck.Result healthy()
    {
        return new HealthCheckResult( true );
    }

    public static HealthCheck.Result unhealthy( String message )
    {
        return new HealthCheckResult( false ).withMessage( message );
    }

    public static HealthCheck.Result unhealthy( Throwable err )
    {
        return new HealthCheckResult( false ).withThrowable( err );
    }

    @Override
    public boolean isHealthy()
    {
        return healthy;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public Throwable getError()
    {
        return err;
    }

    @Override
    public String getTimestamp()
    {
        return new Date().toString();
    }
}
