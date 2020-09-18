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
package org.commonjava.o11yphant.honeycomb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.o11yphant.metrics.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.CONTENT_TRACKING_ID;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.HTTP_METHOD;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.HTTP_STATUS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.PACKAGE_TYPE;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.PATH;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.TRACE_ID;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REQUEST_LATENCY_MILLIS;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.REST_ENDPOINT_PATH;

public interface HoneycombConfiguration
{
    Set<String> DEFAULT_FIELDS = Collections.unmodifiableSet(
                    new HashSet<>( Arrays.asList( CONTENT_TRACKING_ID, HTTP_METHOD, HTTP_STATUS, TRACE_ID, CLIENT_ADDR,
                                                  PATH, PACKAGE_TYPE, REST_ENDPOINT_PATH, REQUEST_LATENCY_MILLIS ) ) );

    Map<String, Integer> getSpanRates();

    boolean isEnabled();

    String getServiceName();

    String getWriteKey();

    String getDataset();

    Integer getBaseSampleRate();

    Set<String> getFieldSet();

    String getEnvironmentMappings();

    String getCPNames();

    String getNodeId();

    default int getSampleRate( Method method )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        Map<String, Integer> spanRates = getSpanRates();
        if ( !spanRates.isEmpty() )
        {
            String[] keys = { method.getName(), method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                            method.getDeclaringClass().getName() + "." + method.getName(),
                            method.getDeclaringClass().getSimpleName(), method.getDeclaringClass().getName() };

            for ( String key : keys )
            {
                Integer rate = spanRates.get( key );
                if ( rate != null )
                {
                    logger.trace( "Found sampling rate for: {} = {}", key, rate );
                    return rate;
                }
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", method, getBaseSampleRate() );
        return getBaseSampleRate();
    }

    default Integer getSampleRate( final String classifier )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        Map<String, Integer> spanRates = getSpanRates();
        Integer rate = spanRates.get( classifier );
        if ( rate != null )
        {
            logger.trace( "Found sampling rate for: {} = {}", classifier, rate );
            return rate;
        }

        String[] parts = classifier.split( "\\." );
        for ( String part : parts )
        {
            rate = spanRates.get( part );
            if ( rate != null )
            {
                logger.trace( "Found sampling rate for: {} = {}", part, rate );
                return rate;
            }
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = parts.length; i > 0; i-- )
        {
            sb.setLength( 0 );
            for ( int j = 0; j < i; j++ )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( '.' );
                }
                sb.append( parts[j] );
            }

            rate = spanRates.get( sb.toString() );
            if ( rate != null )
            {
                logger.trace( "Found sampling rate for: {} = {}", sb, rate );
                return rate;
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", classifier, getBaseSampleRate() );
        return getBaseSampleRate();
    }

    default boolean isConsoleTransport()
    {
        return false;
    }
}
