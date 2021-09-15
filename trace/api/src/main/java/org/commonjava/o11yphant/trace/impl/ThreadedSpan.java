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
package org.commonjava.o11yphant.trace.impl;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.thread.ThreadedTraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class ThreadedSpan
    extends SpanWrapper
{
    private final Optional<SpanAdapter> parentSpan;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public ThreadedSpan( SpanAdapter span, Optional<SpanAdapter> parentSpan )
    {
        super( span );
        this.parentSpan = parentSpan;
    }

    @Override
    public void close()
    {
        SpanAdapter span = getDelegate();
        parentSpan.ifPresent( parent->{
            Map<String, Object> localFields = span.getFields();
            try
            {
                parent.getInProgressFields().forEach( (key,parentVal) -> {
                    Object localVal = localFields.get( key );
                    if ( localVal == null )
                    {
                        span.setInProgressField( key, parentVal );
                    }
                    else if ( parentVal instanceof Long )
                    {
                        span.setInProgressField( key, ( (Long) localVal + (Long) parentVal ) );
                    }
                    else if ( parentVal instanceof Integer )
                    {
                        span.setInProgressField( key, ( (Integer) localVal + (Integer) parentVal ) );
                    }
                    else if ( parentVal instanceof Double )
                    {
                        span.setInProgressField( key, ( (Double) localVal + (Double) parentVal ) );
                    }
                });
            }
            catch ( Throwable t )
            {
                logger.error( "Failed to propagate cumulative trace metrics back from child to parent spans: "
                                              + t.getLocalizedMessage(), t );
            }
        } );
        span.close();
    }

}
