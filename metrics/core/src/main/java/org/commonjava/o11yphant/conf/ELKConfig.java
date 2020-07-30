package org.commonjava.o11yphant.conf;

public class ELKConfig
{
    private long elkJVMPeriodInSeconds;

    private String elkHosts;

    private String elkIndex;

    private long elkPeriodInSeconds;

    private String elkPrefix;

    private long elkHealthCheckPeriodInSeconds;

    public long getElkJVMPeriodInSeconds()
    {
        return elkJVMPeriodInSeconds;
    }

    public void setElkJVMPeriodInSeconds( long elkJVMPeriodInSeconds )
    {
        this.elkJVMPeriodInSeconds = elkJVMPeriodInSeconds;
    }

    public String getElkHosts()
    {
        return elkHosts;
    }

    public void setElkHosts( String elkHosts )
    {
        this.elkHosts = elkHosts;
    }

    public String getElkIndex()
    {
        return elkIndex;
    }

    public void setElkIndex( String elkIndex )
    {
        this.elkIndex = elkIndex;
    }

    public long getElkPeriodInSeconds()
    {
        return elkPeriodInSeconds;
    }

    public void setElkPeriodInSeconds( long elkPeriodInSeconds )
    {
        this.elkPeriodInSeconds = elkPeriodInSeconds;
    }

    public String getElkPrefix()
    {
        return elkPrefix;
    }

    public void setElkPrefix( String elkPrefix )
    {
        this.elkPrefix = elkPrefix;
    }

    public long getElkHealthCheckPeriodInSeconds()
    {
        return elkHealthCheckPeriodInSeconds;
    }

    public void setElkHealthCheckPeriodInSeconds( long elkHealthCheckPeriodInSeconds )
    {
        this.elkHealthCheckPeriodInSeconds = elkHealthCheckPeriodInSeconds;
    }
}
