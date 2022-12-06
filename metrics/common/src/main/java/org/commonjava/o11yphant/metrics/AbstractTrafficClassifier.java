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

import org.commonjava.cdi.util.weft.ThreadContext;

import java.util.*;

import static java.util.Arrays.asList;

public abstract class AbstractTrafficClassifier
                implements TrafficClassifier
{
    public static final String CACHED_FUNCTIONS = "cached-functions";

    public static final Set<String> MODIFY_METHODS = new HashSet<>( asList( "POST", "PUT", "DELETE" ) );

    protected abstract List<String> calculateCachedFunctionClassifiers( String restPath, String method, Map<String, String> headers);

    public List<String> classifyFunctions( String restPath, String method, Map<String, String> headers )
    {
        Optional<List<String>> cached = getCachedFunctionClassifiers();
        if ( cached.isPresent() )
        {
            return cached.get();
        }

        List<String> result = calculateCachedFunctionClassifiers( restPath, method, headers );

        putCachedFunctionClassifiers( result );
        return result;
    }

    public Optional<List<String>> getCachedFunctionClassifiers()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            Object cached = ctx.get( CACHED_FUNCTIONS );
            if ( cached != null )
            {
                //noinspection unchecked
                return Optional.of( (List<String>) cached );
            }
        }

        return Optional.empty();
    }

    public void putCachedFunctionClassifiers( List<String> result )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            ctx.put( CACHED_FUNCTIONS, result );
        }
    }

}
