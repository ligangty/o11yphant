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
