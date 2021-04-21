package org.commonjava.o11yphant.trace.spi.adapter;

import java.util.Map;

public interface SpanAdapter
{
    boolean isLocalRoot();

    String getTraceId();

    String getSpanId();

    void addField( String name, Object value );

    Map<String, Object> getFields();

    void close();

    void setInProgressField(String key, Object value);

    <V> V getInProgressField( String key, V defValue );

    void clearInProgressField( String key );

    Map<String, Object> getInProgressFields();
}
