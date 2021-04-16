package org.commonjava.o11yphant.jhttpc;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.commonjava.o11yphant.trace.TraceManager.addFieldToActiveSpan;

public class TracerHttpClient
                extends CloseableHttpClient
{
    private final CloseableHttpClient delegate;

    private Optional<TraceManager> traceManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public TracerHttpClient( CloseableHttpClient delegate, Optional<TraceManager> traceManager )
    {
        this.traceManager = traceManager;
        this.delegate = delegate;
    }

    @Override
    protected CloseableHttpResponse doExecute( HttpHost target, HttpRequest request, HttpContext context )
                    throws IOException, ClientProtocolException
    {
        try
        {
            URL url = new URL( request.getRequestLine().getUri() );
            Optional<SpanAdapter> span;
            if ( traceManager.isPresent() )
            {
                span = traceManager.get().startClientRequestSpan(
                                request.getRequestLine().getMethod() + "_" + url.getHost() + "_" + url.getPort(),
                                request );
            }
            else
            {
                span = Optional.empty();
            }

            addFieldToActiveSpan( "target-url", request.getRequestLine().getUri() );
            CloseableHttpResponse response = delegate.execute( target, request, context );
            if ( response != null )
            {
                addFieldToActiveSpan( "target-response", response.getStatusLine().getStatusCode() );
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
        }

        // should never reach!
        return null;
    }

    @Override
    public void close() throws IOException
    {
        delegate.close();
    }

    @Override
    public HttpParams getParams()
    {
        return delegate.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager()
    {
        return delegate.getConnectionManager();
    }
}
