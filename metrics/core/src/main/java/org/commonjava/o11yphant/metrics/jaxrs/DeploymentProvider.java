package org.commonjava.o11yphant.metrics.jaxrs;

import io.undertow.servlet.api.DeploymentInfo;

public interface DeploymentProvider
{
    DeploymentInfo getDeploymentInfo( String contextRoot );
}
