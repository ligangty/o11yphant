package org.commonjava.o11yphant.api;

public interface Timer
                extends Metric, Metered
{
    Context time();

    interface Context
                    extends AutoCloseable
    {
        long stop();
    }
}