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
package org.commonjava.o11yphant.metrics.jaxrs;

import com.codahale.metrics.servlets.HealthCheckServlet;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.commonjava.o11yphant.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.DefaultMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CodahaleHealthCheckDeploymentProvider
                implements HealthCheckDeploymentProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetricsConfig config;

    @Inject
    private DefaultMetricsManager metricsManager;

    public DeploymentInfo getDeploymentInfo( String contextRoot )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        final ServletInfo servlet = Servlets.servlet( "healthcheck", HealthCheckServlet.class )
                                            .addMapping( "/healthcheck" )
                                            .addMapping( "/healthchecks" );

        final DeploymentInfo di = new DeploymentInfo().addListener(
                        Servlets.listener( CodahaleHealthCheckServletContextListener.class ) )
                                                      .setContextPath( contextRoot )
                                                      .addServlet( servlet )
                                                      .setDeploymentName( "HealthCheck Deployment" )
                                                      .setClassLoader( ClassLoader.getSystemClassLoader() );

        logger.info( "Returning deployment info for health check" );
        return di;
    }
}
