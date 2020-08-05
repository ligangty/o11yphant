package org.commonjava.o11yphant.metrics.api;

public interface Meter
                extends Metric, Metered
{
    void mark();

    void mark( long n );
}
