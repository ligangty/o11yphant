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
package org.commonjava.o11yphant.otel;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class OtelTracePluginTest
{
    private final Logger logger = LoggerFactory.getLogger( getClass().getName());

    @Test
    public void testStartup() throws InterruptedException
    {
        TracerConfiguration minTraceConfig = new TracerConfiguration()
        {
            @Override
            public boolean isEnabled()
            {
                return true;
            }

            @Override
            public boolean isConsoleTransport()
            {
                return true;
            }

            @Override
            public String getServiceName()
            {
                return "service";
            }

            @Override
            public String getNodeId()
            {
                return "node";
            }
        };

        OtelConfiguration oConfig = new OtelConfiguration()
        {
        };

        logger.info( "Starting Otel plugin..." );
        LoggingSpanExporter exporter = LoggingSpanExporter.create();
        OtelTracePlugin plugin = new OtelTracePlugin( minTraceConfig, oConfig, exporter );

        for ( int i = 0; i < 100; i++ )
        {
            SpanAdapter span = plugin.getSpanProvider().startServiceRootSpan( "test-span", Optional.empty() );
            span.addField( "i", i );
            span.close();
        }

        logger.info( "Waiting 6s for batch send to run..." );
        Thread.sleep( 6000 );

        exporter.shutdown();
    }
}
