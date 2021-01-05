package org.commonjava.o11yphant.honeycomb;

import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;

public class SimpleTraceSampler
                implements TraceSampler<String>
{
    private HoneycombConfiguration honeycombConfiguration;

    public SimpleTraceSampler( HoneycombConfiguration honeycombConfiguration )
    {
        this.honeycombConfiguration = honeycombConfiguration;
    }

    /**
     * The input is span name.
     */
    @Override
    public int sample( String input )
    {
        return honeycombConfiguration.getSampleRate( input );
    }
}
