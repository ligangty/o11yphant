package org.commonjava.o11yphant.honeycomb;

import java.util.Map;
import java.util.function.Supplier;

/**
 * These are to be injected into root spans for a node (not necessarily restricted to the first span in a trace,
 * more like the first span in a service). RootSpanFields instances should be iterated with the root span when a service is finished executing (or on error!).
 */
public interface RootSpanFields extends Supplier<Map<String,Object>>
{
}
