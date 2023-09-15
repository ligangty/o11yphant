/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
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

import org.commonjava.o11yphant.trace.TracerConfiguration;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Take the node-id from honeycomb configuration and inject it as a field into the root span.
 */
@ApplicationScoped
public class ConfiguredSpanFieldsInjector
                implements SpanFieldsInjector
{
    @Inject
    private TracerConfiguration configuration;

    @Override
    public void decorateSpanAtStart( SpanAdapter span )
    {
        span.addField( "config.node.id", configuration.getNodeId() );
    }
}
