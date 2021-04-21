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
