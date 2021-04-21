package org.commonjava.o11yphant.jhttpc;

import org.apache.http.Header;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.HttpFactoryIfc;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SpanningHttpFactory
                implements HttpFactoryIfc
{
    private HttpFactory delegate;

    private Optional<TraceManager> traceManager;

    public SpanningHttpFactory( HttpFactory httpFactory, Optional<TraceManager> traceManager )
    {
        super();
        delegate = httpFactory;
        this.traceManager = traceManager;
    }

    public PasswordManager getPasswordManager()
    {
        return delegate.getPasswordManager();
    }

    @Override
    public CloseableHttpClient createClient() throws JHttpCException
    {
        return new TracerHttpClient( delegate.createClient(), traceManager );
    }

    @Override
    public CloseableHttpClient createClient( SiteConfig location ) throws JHttpCException
    {
        return new TracerHttpClient( delegate.createClient( location ), traceManager );
    }

    @Override
    public CloseableHttpClient createClient( SiteConfig location, List<Header> defaultHeaders ) throws JHttpCException
    {
        return new TracerHttpClient( delegate.createClient( location, defaultHeaders ), traceManager );
    }

    @Override
    public HttpClientContext createContext() throws JHttpCException
    {
        return delegate.createContext();
    }

    @Override
    public HttpClientContext createContext( SiteConfig location ) throws JHttpCException
    {
        return delegate.createContext( location );
    }

    @Override
    public void close() throws IOException
    {
        delegate.close();
    }

    @Override
    public boolean isShutdown()
    {
        return delegate.isShutdown();
    }

    @Override
    public boolean shutdownNow()
    {
        return delegate.shutdownNow();
    }

    @Override
    public boolean shutdownGracefully( long timeoutMillis ) throws InterruptedException
    {
        return delegate.shutdownGracefully( timeoutMillis );
    }
}
