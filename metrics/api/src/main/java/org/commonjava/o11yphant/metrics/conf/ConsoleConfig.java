package org.commonjava.o11yphant.metrics.conf;

public class ConsoleConfig
{
    private long consolePeriodInSeconds;

    public void setConsolePeriodInSeconds( long consolePeriodInSeconds )
    {
        this.consolePeriodInSeconds = consolePeriodInSeconds;
    }

    public long getConsolePeriodInSeconds()
    {
        return consolePeriodInSeconds;
    }
}
