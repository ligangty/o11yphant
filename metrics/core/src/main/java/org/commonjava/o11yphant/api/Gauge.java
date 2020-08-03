package org.commonjava.o11yphant.api;

@FunctionalInterface
public interface Gauge<T> extends Metric
{
    T getValue();
}
