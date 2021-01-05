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
package org.commonjava.o11yphant.honeycomb.impl;

import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Take the node-id from honeycomb configuration and inject it as a field into the root span.
 */
@ApplicationScoped
public class ConfiguredRootSpanFields
                implements RootSpanFields
{
    @Inject
    private HoneycombConfiguration configuration;

    private final Map<String, Object> configured = new HashMap<>();

    @PostConstruct
    public void init()
    {
        configured.put( "config.node.id", configuration.getNodeId() );
    }

    @Override
    public Map<String, Object> get()
    {
        return configured;
    }
}
