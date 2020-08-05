package org.commonjava.o11yphant.metrics.api;

@FunctionalInterface
public interface Gauge<T> extends Metric
{
    T getValue();
}
