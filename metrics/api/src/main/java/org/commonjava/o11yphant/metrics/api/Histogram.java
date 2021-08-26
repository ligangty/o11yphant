package org.commonjava.o11yphant.metrics.api;

public interface Histogram extends Metric
{
    void update( int value );

    void update( long value );

    long getCount();

    Snapshot getSnapshot();
}
