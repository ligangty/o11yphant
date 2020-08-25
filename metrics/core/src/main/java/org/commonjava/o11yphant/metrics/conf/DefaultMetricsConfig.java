package org.commonjava.o11yphant.metrics.conf;

import javax.enterprise.inject.Alternative;

@Alternative
public class DefaultMetricsConfig
                implements MetricsConfig
{
    @Override
    public String getNodePrefix()
    {
        return "";
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public String getReporter()
    {
        return null;
    }

    @Override
    public ConsoleConfig getConsoleConfig()
    {
        return null;
    }

    @Override
    public GraphiteConfig getGraphiteConfig()
    {
        return null;
    }

    @Override
    public ELKConfig getELKConfig()
    {
        return null;
    }

    @Override
    public int getMeterRatio()
    {
        return 0;
    }
}
