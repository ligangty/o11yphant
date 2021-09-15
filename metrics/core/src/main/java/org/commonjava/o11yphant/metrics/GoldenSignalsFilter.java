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
package org.commonjava.o11yphant.metrics;

import org.commonjava.o11yphant.metrics.sli.GoldenSignalsFunctionMetrics;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.GOLDEN_SIGNALS_FUNCTIONS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_NS;

@ApplicationScoped
public class GoldenSignalsFilter
    implements Filter
{
    @Inject
    private GoldenSignalsMetricSet metricSet;

    @Inject
    private TrafficClassifier classifier;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public GoldenSignalsFilter( final GoldenSignalsMetricSet metricSet, AbstractTrafficClassifier classifier )
    {
        this.metricSet = metricSet;
        this.classifier = classifier;
    }

    public GoldenSignalsFilter() {}

    @Override
    public void init( final FilterConfig filterConfig )
    {
    }

    @Override
    public void doFilter( final ServletRequest servletRequest, final ServletResponse servletResponse,
                          final FilterChain filterChain )
            throws IOException, ServletException
    {
        logger.trace( "START: {}", getClass().getSimpleName() );

        long start = System.nanoTime();

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        String path = req.getPathInfo();
        Map<String, String> headers = Collections.list(req.getHeaderNames()).stream()
                                                .collect( Collectors.toMap( h -> h, req::getHeader));
        Collection<String> functions = classifier.classifyFunctions( path, req.getMethod(), headers );
        logger.debug( "Get classified golden functions, path: {}, method: {}, functions: {}", path, req.getMethod(), functions );

        RequestContextHelper.setContext( GOLDEN_SIGNALS_FUNCTIONS, functions );
        try
        {
            functions.forEach( function -> metricSet.function( function ).ifPresent(
                    GoldenSignalsFunctionMetrics::started ) );
        }
        catch ( Exception e )
        {
            logger.error( "Failed to classify / measure load for: " + path, e );
        }

        try
        {
            filterChain.doFilter( req, resp );
        }
        catch ( IOException | ServletException | RuntimeException e )
        {
            functions.forEach( function -> metricSet.function( function )
                                                    .ifPresent( GoldenSignalsFunctionMetrics::error ) );
            throw e;
        }
        finally
        {
            // In some cases, we cannot calculate latency without capturing actual data transfer time. When this happens,
            // we can capture the actual data transfer time and subtract it from the total execution time to get
            // latency.
            long end = RequestContextHelper.getRequestEndNanos() - RequestContextHelper.getRawIoWriteNanos();

            RequestContextHelper.setContext( REQUEST_LATENCY_NS, end - start );
            RequestContextHelper.setContext( REQUEST_LATENCY_MILLIS, (end-start) / NANOS_PER_MILLISECOND  );

            boolean error = resp.getStatus() > 499;

            functions.forEach( function -> metricSet.function( function ).ifPresent( functionMetrics -> {
                functionMetrics.latency( end-start ).call();
                if ( error )
                {
                    functionMetrics.error();
                }
            } ) );

            logger.trace( "END: {}", getClass().getSimpleName() );
        }
    }

    @Override
    public void destroy()
    {
    }

}
