package org.commonjava.o11yphant.otel;

public interface OtelConfiguration
{
    default String getInstrumentationName()
    {
        return "O11yphant";
    }

    default String getInstrumentationVersion()
    {
        return "1.0";
    }
}
