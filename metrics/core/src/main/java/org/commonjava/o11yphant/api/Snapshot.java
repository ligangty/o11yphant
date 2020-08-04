package org.commonjava.o11yphant.api;

public interface Snapshot
{
    double getValue(double var1);

    long[] getValues();

    int size();

    default double getMedian() {
        return this.getValue(0.5D);
    }

    default double get75thPercentile() {
        return this.getValue(0.75D);
    }

    default double get95thPercentile() {
        return this.getValue(0.95D);
    }

    default double get98thPercentile() {
        return this.getValue(0.98D);
    }

    default double get99thPercentile() {
        return this.getValue(0.99D);
    }

    default double get999thPercentile() {
        return this.getValue(0.999D);
    }

    long getMax();

    double getMean();

    long getMin();

    double getStdDev();

}
