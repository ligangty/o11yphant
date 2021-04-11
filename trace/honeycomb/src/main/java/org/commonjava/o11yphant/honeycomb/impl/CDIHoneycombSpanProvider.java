package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.context.TracingContext;
import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import io.honeycomb.libhoney.EventPostProcessor;
import org.commonjava.o11yphant.honeycomb.HoneycombConfiguration;
import org.commonjava.o11yphant.trace.TracerConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class CDIHoneycombSpanProvider extends HoneycombSpanProvider
{
    @Inject
    private Instance<CustomTraceIdProvider> traceIdProviderInstance;

    @Inject
    public CDIHoneycombSpanProvider( HoneycombConfiguration configuration, TracerConfiguration tracerConfiguration,
                                     TraceSampler traceSampler, TracingContext tracingContext,
                                     EventPostProcessor eventPostProcessor )
    {
        super( configuration, tracerConfiguration, traceSampler, tracingContext, eventPostProcessor);
    }

    @Override
    protected CustomTraceIdProvider getCustomTraceIdProvider()
    {
        if ( traceIdProviderInstance != null && !traceIdProviderInstance.isUnsatisfied() )
        {
            return traceIdProviderInstance.get();
        }
        return null;
    }

}
