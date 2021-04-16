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
package org.commonjava.o11yphant.trace;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.adapter.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ApplicationScoped
public class TraceFilter
                implements Filter
{
    @Inject
    private TraceManager traceManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig )
    {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
                    throws IOException, ServletException
    {
        logger.trace( "START: {}", getClass().getSimpleName() );

        HttpServletRequest hsr = (HttpServletRequest) request;
        logger.debug( "START: {}", hsr.getPathInfo() );

        Optional<SpanAdapter> rootSpan = null;
        try
        {
            Optional<SpanContext> spanContext = traceManager.extractContext( hsr );
            rootSpan = traceManager.startServletRootSpan( getEndpointName( hsr.getMethod(), hsr.getPathInfo() ), hsr );
            if ( rootSpan != null )
            {
                addRequestFields( hsr );
            }

            chain.doFilter( request, response );
        }
        finally
        {
            logger.debug( "END: {}", hsr.getPathInfo() );
            HttpServletResponse resp = (HttpServletResponse) response;
            traceManager.addSpanField( "status_code", Integer.toString( resp.getStatus() ) );
            rootSpan.ifPresent( SpanAdapter::close );

            logger.trace( "END: {}", getClass().getSimpleName() );
        }
    }

    private String getEndpointName( String method, String pathInfo )
    {
        StringBuilder sb = new StringBuilder( method + "_" );
        String[] toks = pathInfo.split( "/" );
        for ( String s : toks )
        {
            if ( isBlank( s ) || "api".equals( s ) )
            {
                continue;
            }
            sb.append( s );
            if ( "admin".equals( s ) )
            {
                sb.append( "_" );
            }
            else
            {
                break;
            }
        }
        return sb.toString();
    }

    private void addRequestFields( HttpServletRequest request )
    {
        traceManager.addSpanField( "path_info", request.getPathInfo() );
    }

    @Override
    public void destroy()
    {
    }

}
