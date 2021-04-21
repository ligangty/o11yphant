package org.commonjava.o11yphant.jhttpc;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

public class SpanClosingResponse
                implements CloseableHttpResponse
{
    private CloseableHttpResponse delegate;

    private Optional<SpanAdapter> span;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public SpanClosingResponse( CloseableHttpResponse delegate, Optional<SpanAdapter> span )
    {
        this.delegate = delegate;
        this.span = span;
    }

    @Override
    public StatusLine getStatusLine()
    {
        return delegate.getStatusLine();
    }

    @Override
    public void setStatusLine( StatusLine statusLine )
    {
        delegate.setStatusLine( statusLine );
    }

    @Override
    public void setStatusLine( ProtocolVersion protocolVersion, int i )
    {
        delegate.setStatusLine( protocolVersion, i );
    }

    @Override
    public void setStatusLine( ProtocolVersion protocolVersion, int i, String s )
    {
        delegate.setStatusLine( protocolVersion, i, s );
    }

    @Override
    public void setStatusCode( int i ) throws IllegalStateException
    {
        delegate.setStatusCode( i );
    }

    @Override
    public void setReasonPhrase( String s ) throws IllegalStateException
    {
        delegate.setReasonPhrase( s );
    }

    @Override
    public HttpEntity getEntity()
    {
        return delegate.getEntity();
    }

    @Override
    public void setEntity( HttpEntity httpEntity )
    {
        delegate.setEntity( httpEntity );
    }

    @Override
    public Locale getLocale()
    {
        return delegate.getLocale();
    }

    @Override
    public void setLocale( Locale locale )
    {
        delegate.setLocale( locale );
    }

    @Override
    public ProtocolVersion getProtocolVersion()
    {
        return delegate.getProtocolVersion();
    }

    @Override
    public boolean containsHeader( String s )
    {
        return delegate.containsHeader( s );
    }

    @Override
    public Header[] getHeaders( String s )
    {
        return delegate.getHeaders( s );
    }

    @Override
    public Header getFirstHeader( String s )
    {
        return delegate.getFirstHeader( s );
    }

    @Override
    public Header getLastHeader( String s )
    {
        return delegate.getLastHeader( s );
    }

    @Override
    public Header[] getAllHeaders()
    {
        return delegate.getAllHeaders();
    }

    @Override
    public void addHeader( Header header )
    {
        delegate.addHeader( header );
    }

    @Override
    public void addHeader( String s, String s1 )
    {
        delegate.addHeader( s, s1 );
    }

    @Override
    public void setHeader( Header header )
    {
        delegate.setHeader( header );
    }

    @Override
    public void setHeader( String s, String s1 )
    {
        delegate.setHeader( s, s1 );
    }

    @Override
    public void setHeaders( Header[] headers )
    {
        delegate.setHeaders( headers );
    }

    @Override
    public void removeHeader( Header header )
    {
        delegate.removeHeader( header );
    }

    @Override
    public void removeHeaders( String s )
    {
        delegate.removeHeaders( s );
    }

    @Override
    public HeaderIterator headerIterator()
    {
        return delegate.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator( String s )
    {
        return delegate.headerIterator( s );
    }

    @Override
    @Deprecated
    public HttpParams getParams()
    {
        return delegate.getParams();
    }

    @Override
    @Deprecated
    public void setParams( HttpParams httpParams )
    {
        delegate.setParams( httpParams );
    }

    @Override
    public void close() throws IOException
    {
        delegate.close();
        try
        {
            span.ifPresent( s->s.close() );
        }
        catch ( Throwable t )
        {
            logger.error( "Failed to close http response span: " + t.getLocalizedMessage(), t );
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
        }
    }
}
