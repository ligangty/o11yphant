package org.commonjava.o11yphant.otel;

import java.util.HashMap;
import java.util.Map;

public interface OtelConfiguration
{
    String DEFAULT_GRPC_URI = "http://localhost:55680";

    default String getInstrumentationName()
    {
        return "O11yphant";
    }

    default String getInstrumentationVersion()
    {
        return "1.0";
    }

    default Map<String, String> getGrpcHeaders(){
        return new HashMap<>();
    }

    default String getGrpcEndpointUri()
    {
        return DEFAULT_GRPC_URI;
    }
}
