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
package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.metrics.api.Snapshot;
import org.commonjava.o11yphant.metrics.api.Timer;

import java.util.concurrent.TimeUnit;

public class O11Timer
                implements Timer
{
    private final com.codahale.metrics.Timer codahaleTimer;

    public O11Timer()
    {
        codahaleTimer = new com.codahale.metrics.Timer();
    }

    public O11Timer( com.codahale.metrics.Timer timer )
    {
        codahaleTimer = timer;
    }

    @Override
    public long getCount()
    {
        return codahaleTimer.getCount();
    }

    @Override
    public double getFifteenMinuteRate()
    {
        return codahaleTimer.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate()
    {
        return codahaleTimer.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate()
    {
        return codahaleTimer.getMeanRate();
    }

    @Override
    public double getOneMinuteRate()
    {
        return codahaleTimer.getOneMinuteRate();
    }

    @Override
    public Context time()
    {
        return new O11Context( codahaleTimer.time() );
    }

    @Override
    public void update( long duration, TimeUnit timeUnit )
    {
        codahaleTimer.update( duration, timeUnit );
    }

    @Override
    public Snapshot getSnapshot()
    {
        final com.codahale.metrics.Snapshot codehaleSnapshot = codahaleTimer.getSnapshot();
        return new O11Snapshot( codehaleSnapshot);
    }

    public static class O11Context
                    implements Context
    {
        private final com.codahale.metrics.Timer.Context codahaleContext;

        public O11Context( com.codahale.metrics.Timer.Context context )
        {
            this.codahaleContext = context;
        }

        @Override
        public long stop()
        {
            return codahaleContext.stop();
        }

        @Override
        public void close()
        {
            stop();
        }
    }

    public com.codahale.metrics.Timer getCodahaleTimer()
    {
        return codahaleTimer;
    }
}
