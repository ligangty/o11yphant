/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
