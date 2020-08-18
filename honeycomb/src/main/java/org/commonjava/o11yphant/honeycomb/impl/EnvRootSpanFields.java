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
