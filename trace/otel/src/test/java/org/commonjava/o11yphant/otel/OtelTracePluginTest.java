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
        LoggingSpanExporter exporter = new LoggingSpanExporter();
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
