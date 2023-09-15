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
package org.commonjava.o11yphant.honeycomb.impl;

import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.o11yphant.trace.TracerConfiguration;

public class SimpleTraceSampler
                implements TraceSampler<String>
{
    private final TracerConfiguration tracerConfiguration;

    public SimpleTraceSampler( TracerConfiguration tracerConfiguration )
    {
        this.tracerConfiguration = tracerConfiguration;
    }

    /**
     * The input is span name.
     */
    @Override
    public int sample( String input )
    {
        return tracerConfiguration.getBaseSampleRate();
    }
}
