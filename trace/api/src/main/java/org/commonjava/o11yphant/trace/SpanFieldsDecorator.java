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
package org.commonjava.o11yphant.trace;

import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings( "unused" )
public class SpanFieldsDecorator
{
    private List<SpanFieldsInjector> spanFieldInjectors = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger( getClass().getName());

    protected SpanFieldsDecorator()
    {
    }

    public SpanFieldsDecorator( List<SpanFieldsInjector> spanFieldInjectors )
    {
        this.spanFieldInjectors = spanFieldInjectors;
    }

    protected void registerRootSpanFields( List<SpanFieldsInjector> spanFieldInjectors )
    {
        this.spanFieldInjectors = spanFieldInjectors;
    }

    public final void decorateOnStart( SpanAdapter span )
    {
        spanFieldInjectors.forEach( injectSpanFields -> {
            logger.trace( "START: Injecting fields from: {} to: {}", injectSpanFields, span );
            injectSpanFields.decorateSpanAtStart( span );
        } );
    }

    public final void decorateOnClose( SpanAdapter span )
    {
        spanFieldInjectors.forEach( injectSpanFields -> {
            logger.trace( "CLOSE: Injecting fields from: {} to: {}", injectSpanFields, span );
            injectSpanFields.decorateSpanAtClose( span );
        } );
    }
}
