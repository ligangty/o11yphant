package org.commonjava.o11yphant.metrics.impl;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import org.commonjava.o11yphant.metrics.api.Histogram;
import org.commonjava.o11yphant.metrics.api.Snapshot;

public class O11Histogram implements Histogram
{
    private com.codahale.metrics.Histogram codehaleHistogram;

    public O11Histogram()
    {
        this.codehaleHistogram = new com.codahale.metrics.Histogram( new ExponentiallyDecayingReservoir() );
    }

    public O11Histogram( com.codahale.metrics.Histogram codehaleHistogram )
    {
        this.codehaleHistogram = codehaleHistogram;
    }
    @Override
    public void update( int value )
    {
        codehaleHistogram.update( value );
    }

    @Override
    public void update( long value )
    {
        codehaleHistogram.update( value );
    }

    @Override
    public long getCount()
    {
        return codehaleHistogram.getCount();
    }

    @Override
    public Snapshot getSnapshot()
    {
        return new O11Snapshot( codehaleHistogram.getSnapshot() );
    }

    public com.codahale.metrics.Histogram getCodehaleHistogram()
    {
        return codehaleHistogram;
    }
}
