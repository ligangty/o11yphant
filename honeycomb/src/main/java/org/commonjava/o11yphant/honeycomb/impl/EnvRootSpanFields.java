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

import org.apache.commons.lang.StringUtils;
import org.commonjava.o11yphant.honeycomb.RootSpanFields;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Allow configuration of a set of environment variables to extract from the system and add to the Span as fields.
 */
@ApplicationScoped
public class EnvRootSpanFields
                implements RootSpanFields
{
    @Inject
    private HoneycombConfiguration configuration;

    @Override
    public Map<String, Object> get()
    {
        String environmentMappings = configuration.getEnvironmentMappings();

        String[] mappings = environmentMappings == null ? new String[0] : environmentMappings.split( "\\s*,\\s*" );
        Map<String, Object> envars = new HashMap<>();
        Stream.of( mappings ).forEach( kv -> {
            String[] keyAlias = kv.split( "\\s*=\\s*" );
            if ( keyAlias.length > 1 )
            {
                String value = System.getenv( keyAlias[0].trim() );
                if ( StringUtils.isEmpty( value ) )
                {
                    value = "Unknown";
                }

                envars.put( keyAlias[1].trim(), value );
            }
        } );
        return envars;
    }
}
