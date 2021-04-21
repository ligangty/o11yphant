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
package org.commonjava.o11yphant.trace.impl;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static javax.naming.InitialContext.doLookup;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

/**
 * Capture the metrics available for our JDBC connection pools and provide them to the root span, keyed by connection-pool name.
 * We should be able to monitor connections active vs. available, along with system-wide latency stats.
 */
@ApplicationScoped
public class DBConnectionSpanFieldsInjector
                implements SpanFieldsInjector
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TracerConfiguration configuration;

    private Map<String, AgroalDataSourceMetrics> dataSourceMetricsMap = new HashMap<>();

    @PostConstruct
    public void init()
    {
        String cpNames = configuration.getCPNames();
        if ( isBlank( cpNames ) )
        {
            logger.info( "No connection pool names defined" );
            return;
        }

        String[] names = cpNames.split( "," );
        for ( String s : names )
        {
            String name = s.trim();
            String jndiName = "java:/comp/env/jdbc/" + name;
            DataSource ds;
            try
            {
                ds = doLookup( jndiName );
            }
            catch ( NamingException e )
            {
                logger.error( "Failed to lookup jndi name {}", jndiName );
                continue;
            }

            if ( ds instanceof AgroalDataSource )
            {
                AgroalDataSource agroalDataSource = (AgroalDataSource) ds;
                dataSourceMetricsMap.put( "cp." + name, agroalDataSource.getMetrics() );
            }
            else
            {
                logger.warn( "Ignore non-agroal datasource {}", jndiName );
            }
        }
    }

    @Override
    public void decorateSpanAtClose( SpanAdapter span )
    {
        if ( dataSourceMetricsMap.isEmpty() )
        {
            return;
        }

        dataSourceMetricsMap.forEach( ( name, metrics ) -> {
            span.addField( name( name, "acquireCount" ), metrics.acquireCount() );
            span.addField( name( name, "creationCount" ), metrics.creationCount() );
            span.addField( name( name, "leakDetectionCount" ), metrics.leakDetectionCount() );
            span.addField( name( name, "destroyCount" ), metrics.destroyCount() );
            span.addField( name( name, "flushCount" ), metrics.flushCount() );
            span.addField( name( name, "invalidCount" ), metrics.invalidCount() );
            span.addField( name( name, "reapCount" ), metrics.reapCount() );
            span.addField( name( name, "activeCount" ), metrics.activeCount() );
            span.addField( name( name, "availableCount" ), metrics.availableCount() );
            span.addField( name( name, "maxUsedCount" ), metrics.maxUsedCount() );
            span.addField( name( name, "awaitingCount" ), metrics.awaitingCount() );
            span.addField( name( name, "blockingTimeAverage" ), metrics.blockingTimeAverage() );
            span.addField( name( name, "blockingTimeMax" ), metrics.blockingTimeMax() );
            span.addField( name( name, "blockingTimeTotal" ), metrics.blockingTimeTotal() );
            span.addField( name( name, "creationTimeAverage" ), metrics.creationTimeAverage() );
            span.addField( name( name, "creationTimeMax" ), metrics.creationTimeMax() );
            span.addField( name( name, "creationTimeTotal" ), metrics.creationTimeTotal() );
        } );
    }
}
