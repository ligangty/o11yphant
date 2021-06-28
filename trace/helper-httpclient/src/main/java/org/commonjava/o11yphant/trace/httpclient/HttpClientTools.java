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
package org.commonjava.o11yphant.trace.httpclient;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class HttpClientTools
{
    public static Supplier<Map<String, String>> contextExtractor( HttpRequest inbound )
    {
        return ()->{
            Map<String, String> ret = new HashMap<>();
            Header[] headers = inbound.getAllHeaders();
            for ( Header h: headers )
            {
                ret.putIfAbsent( h.getName(), h.getValue() );
            }

            return ret;
        };
    }

    public static BiConsumer<String, String> contextInjector( HttpRequest outbound )
    {
        return outbound::setHeader;
    }
}
