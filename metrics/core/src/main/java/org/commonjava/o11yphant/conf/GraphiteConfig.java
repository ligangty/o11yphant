package org.commonjava.o11yphant.conf;

public class GraphiteConfig
{
    private String graphiteHostName;

    private int graphitePort;

    private String graphitePrefix;

    private long graphitePeriodInSeconds;

    private long graphiteJVMPeriodInSeconds;

    public String getGraphiteHostName()
    {
        return graphiteHostName;
    }

    public void setGraphiteHostName( String graphiteHostName )
    {
        this.graphiteHostName = graphiteHostName;
    }

    public int getGraphitePort()
    {
        return graphitePort;
    }

    public void setGraphitePort( int graphitePort )
    {
        this.graphitePort = graphitePort;
    }

    public String getGraphitePrefix()
    {
        return graphitePrefix;
    }

    public void setGraphitePrefix( String graphitePrefix )
    {
        this.graphitePrefix = graphitePrefix;
    }

    public long getGraphitePeriodInSeconds()
    {
        return graphitePeriodInSeconds;
    }

    public void setGraphitePeriodInSeconds( long graphitePeriodInSeconds )
    {
        this.graphitePeriodInSeconds = graphitePeriodInSeconds;
    }

    public long getGraphiteJVMPeriodInSeconds()
    {
        return graphiteJVMPeriodInSeconds;
    }

    public void setGraphiteJVMPeriodInSeconds( long graphiteJVMPeriodInSeconds )
    {
        this.graphiteJVMPeriodInSeconds = graphiteJVMPeriodInSeconds;
    }
}
