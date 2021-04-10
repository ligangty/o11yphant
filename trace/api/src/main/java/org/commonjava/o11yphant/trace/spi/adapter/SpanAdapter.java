package org.commonjava.o11yphant.trace.spi.adapter;

public interface SpanAdapter
{
    String getTraceId();

    String getSpanId();

    void addField( String name, Object value );

    void close();

    void setInProgressField(String key, Object value);

    <V> V getInProgressField( String key, V defValue );

    void clearInProgressField( String key );
}
