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
