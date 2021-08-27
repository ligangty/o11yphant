package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.metrics.api.Snapshot;

public class O11Snapshot
                implements Snapshot
{
    private com.codahale.metrics.Snapshot codehaleSnapshot;

    public O11Snapshot( com.codahale.metrics.Snapshot codehaleSnapshot )
    {
        this.codehaleSnapshot = codehaleSnapshot;
    }

    @Override
    public double getValue( double var1 )
    {
        return codehaleSnapshot.getValue( var1 );
    }

    @Override
    public long[] getValues()
    {
        return codehaleSnapshot.getValues();
    }

    @Override
    public int size()
    {
        return codehaleSnapshot.size();
    }

    @Override
    public long getMax()
    {
        return codehaleSnapshot.getMax();
    }

    @Override
    public double getMean()
    {
        return codehaleSnapshot.getMean();
    }

    @Override
    public long getMin()
    {
        return codehaleSnapshot.getMin();
    }

    @Override
    public double getStdDev()
    {
        return codehaleSnapshot.getStdDev();
    }
}
