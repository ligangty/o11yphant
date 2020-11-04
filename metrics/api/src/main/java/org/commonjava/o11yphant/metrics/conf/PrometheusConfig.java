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
package org.commonjava.o11yphant.metrics.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PrometheusConfig
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private List<String> expressedMetrics;

    public List<String> getExpressedMetrics()
    {
        return expressedMetrics;
    }

    public void setExpressedMetrics( List<String> expressedMetrics )
    {
        this.expressedMetrics = expressedMetrics;
    }

    public boolean isMetricExpressed( String name )
    {
        return expressedMetrics.stream().filter( n->{
            String pname = n.replace( '.', '_' );
            if ( name.equals( n ) || name.contains( n ) || pname.equals( n ) || pname.contains( n ) )
            {
                logger.info( "ACCEPT metric: {} for expression: {}", name, n );
                return true;
            }
            logger.info("REJECT metric: {} for expression: {}", name, n );
            return false;
        } ).findFirst().isPresent();
    }
}
