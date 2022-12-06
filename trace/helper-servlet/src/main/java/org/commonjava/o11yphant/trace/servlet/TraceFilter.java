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
package org.commonjava.o11yphant.trace.servlet;

import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
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
import static org.commonjava.o11yphant.trace.servlet.ServletContextTools.contextExtractor;

@ApplicationScoped
public class TraceFilter
                implements Filter
{
    @SuppressWarnings( "rawtypes" )
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

        Optional<SpanAdapter> rootSpan = Optional.empty();
        TraceFilterFieldInjector injector = new TraceFilterFieldInjector( (HttpServletResponse) response );
        try
        {
            rootSpan = traceManager.startServiceRootSpan( getEndpointName( hsr.getMethod(), hsr.getPathInfo() ),
                                                          contextExtractor( hsr ) );

            if ( rootSpan.isPresent() ){
                rootSpan.get().addField( "path_info", hsr.getPathInfo() );
                TraceManager.addCloseBlockingDecorator( rootSpan, injector );
            }

            chain.doFilter( request, response );
        }
        finally
        {
            injector.captureResponse();
            rootSpan.ifPresent( SpanAdapter::close );
            logger.debug( "END: {}", hsr.getPathInfo() );
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

    @Override
    public void destroy()
    {
    }

    public static final class TraceFilterFieldInjector implements CloseBlockingDecorator
    {
        private final Logger logger = LoggerFactory.getLogger(getClass().getName());

        private final HttpServletResponse response;

        private int status = -1;

        public TraceFilterFieldInjector( HttpServletResponse response )
        {
            this.response = response;
        }

        @Override
        public void decorateSpanAtClose( SpanAdapter span )
        {
            logger.trace( "Span close detected. Adding response status code." );
            span.addField( "status_code", status );
        }

        public void captureResponse()
        {
            this.status = response.getStatus();
        }
    }

}
