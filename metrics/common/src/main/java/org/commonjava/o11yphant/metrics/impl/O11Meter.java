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
package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.metrics.api.Meter;

public class O11Meter
                implements Meter
{
    private final com.codahale.metrics.Meter codahaleMeter;

    public O11Meter()
    {
        codahaleMeter = new com.codahale.metrics.Meter();
    }

    public O11Meter( com.codahale.metrics.Meter meter )
    {
        codahaleMeter = meter;
    }

    @Override
    public long getCount()
    {
        return codahaleMeter.getCount();
    }

    @Override
    public double getFifteenMinuteRate()
    {
        return codahaleMeter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate()
    {
        return codahaleMeter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate()
    {
        return codahaleMeter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate()
    {
        return codahaleMeter.getOneMinuteRate();
    }

    @Override
    public void mark()
    {
        codahaleMeter.mark();
    }

    @Override
    public void mark( long n )
    {
        codahaleMeter.mark( n );
    }

    public com.codahale.metrics.Meter getCodahaleMeter()
    {
        return codahaleMeter;
    }
}
