package org.commonjava.o11yphant.metrics.api;

import java.util.concurrent.TimeUnit;

public interface Timer
                extends Metric, Metered
{
    Context time();

    void update( long duration, TimeUnit nanoseconds );

    Snapshot getSnapshot();

    interface Context
                    extends AutoCloseable
    {
        long stop();
    }
}