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
package org.commonjava.o11yphant.jhttpc;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.commonjava.o11yphant.trace.TraceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.commonjava.o11yphant.trace.TraceManager.addFieldToActiveSpan;
import static org.commonjava.o11yphant.trace.httpclient.HttpClientTools.contextInjector;

@SuppressWarnings( "rawtypes" )
public class TracerHttpClient
                extends CloseableHttpClient
{
    private final CloseableHttpClient delegate;

    private final Optional<TraceManager> traceManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public TracerHttpClient( CloseableHttpClient delegate, Optional<TraceManager> traceManager )
    {
        this.traceManager = traceManager;
        this.delegate = delegate;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected CloseableHttpResponse doExecute( HttpHost target, HttpRequest request, HttpContext context )
                    throws IOException
    {
        try
        {
            URL url = new URL( request.getRequestLine().getUri() );
            Optional span;
            if ( traceManager.isPresent() )
            {
                span = traceManager.get()
                                   .startClientRequestSpan(
                                                   request.getRequestLine().getMethod() + "_" + url.getHost() + "_"
                                                                   + url.getPort(), contextInjector( request ) );
            }
            else
            {
                span = Optional.empty();
            }

            addFieldToActiveSpan( "target-http-url", request.getRequestLine().getUri() );
            CloseableHttpResponse response = delegate.execute( target, request, context );
            if ( response != null )
            {
                addFieldToActiveSpan( "target-http-status", response.getStatusLine().getStatusCode() );
            }

            return new SpanClosingResponse( response, span );
        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( Throwable t )
        {
            logger.error( "Failed to execute http request: " + t.getLocalizedMessage(), t );
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
            throw new RuntimeException( "Failed to execute: " + t.getMessage(), t );
        }
    }

    @Override
    public void close() throws IOException
    {
        delegate.close();
    }

    @Override
    @Deprecated
    public HttpParams getParams()
    {
        return delegate.getParams();
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager()
    {
        return delegate.getConnectionManager();
    }
}
