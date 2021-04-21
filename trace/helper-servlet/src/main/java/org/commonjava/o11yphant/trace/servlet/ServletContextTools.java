package org.commonjava.o11yphant.trace.servlet;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServletContextTools
{
    public static Supplier<Map<String, String>> contextExtractor( HttpServletRequest inbound )
    {
        return ()->{
            Map<String, String> ret = new HashMap<>();
            Enumeration<String> headerNames = inbound.getHeaderNames();
            while ( headerNames.hasMoreElements() )
            {
                String name = headerNames.nextElement();
                ret.put( name, inbound.getHeader( name ) );
            }

            return ret;
        };
    }
}
