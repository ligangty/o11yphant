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

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;

import static org.commonjava.o11yphant.trace.TracingConstants.LATENCY_TIMER_PAUSE_KEY;

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
        return new LatencyPauseEntity( delegate.getEntity() );
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

    private class LatencyPauseEntity
                    implements HttpEntity
    {
        private HttpEntity entity;

        public LatencyPauseEntity( HttpEntity entity )
        {
            this.entity = entity;
        }

        @Override
        public boolean isRepeatable()
        {
            return entity.isRepeatable();
        }

        @Override
        public boolean isChunked()
        {
            return entity.isChunked();
        }

        @Override
        public long getContentLength()
        {
            return entity.getContentLength();
        }

        @Override
        public Header getContentType()
        {
            return entity.getContentType();
        }

        @Override
        public Header getContentEncoding()
        {
            return entity.getContentEncoding();
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException
        {
            return new LatencyPauseInputStream( entity.getContent() );
        }

        @Override
        public void writeTo( OutputStream outputStream ) throws IOException
        {
            entity.writeTo( outputStream );
        }

        @Override
        public boolean isStreaming()
        {
            return entity.isStreaming();
        }

        @Override
        @Deprecated
        public void consumeContent() throws IOException
        {
            entity.consumeContent();
        }
    }

    private class LatencyPauseInputStream
                    extends FilterInputStream
    {
        private final long start = System.nanoTime();

        public LatencyPauseInputStream( InputStream content )
        {
            super( content );
        }

        @Override
        public void close() throws IOException
        {
            double elapsed = System.nanoTime() - start;
            TraceManager.getActiveSpan().ifPresent( s -> s.updateInProgressField( LATENCY_TIMER_PAUSE_KEY, elapsed ) );
            super.close();
        }
    }
}
