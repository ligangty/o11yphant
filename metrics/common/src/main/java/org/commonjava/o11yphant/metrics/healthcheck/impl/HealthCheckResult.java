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

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HealthCheckResult
                implements HealthCheck.Result
{
    private boolean healthy;

    private String message;

    private Throwable err;

    private Map<String, Object> details = new HashMap<>();

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

    public HealthCheckResult withDetail( String name, Object detail )
    {
        this.details.put( name, detail );
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

    @Override
    public Map<String, Object> getDetails()
    {
        return details;
    }

    public HealthCheck.Result withDetails( Map<String, Object> details )
    {
        this.details = details;
        return this;
    }
}
