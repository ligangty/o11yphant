package org.commonjava.o11yphant.metrics.api;

public interface Metered
{
    long getCount();

    double getFifteenMinuteRate();

    double getFiveMinuteRate();

    double getMeanRate();

    double getOneMinuteRate();
}
