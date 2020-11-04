package org.commonjava.o11yphant.metrics.jaxrs;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoggingPrometheusServlet
                extends MetricsServlet
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public LoggingPrometheusServlet()
    {
        super();
        logger.debug( "Loaded Prometheus metrics servlet with default constructor" );
    }

    public LoggingPrometheusServlet( CollectorRegistry registry )
    {
        super( registry );
        logger.debug( "Loaded Prometheus metrics servlet with registry constructor" );
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        logger.debug( "Prometheus metrics GET" );
        super.doGet( req, resp );
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        logger.debug( "Prometheus metrics POST" );
        super.doPost( req, resp );
    }
}
