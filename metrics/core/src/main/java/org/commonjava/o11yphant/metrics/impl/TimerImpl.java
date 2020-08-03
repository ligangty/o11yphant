package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.api.Timer;

public class TimerImpl
                implements Timer
{
    private com.codahale.metrics.Timer codahaleTimer;

    public TimerImpl( com.codahale.metrics.Timer timer )
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

    public class O11Context
                    implements Context
    {
        private com.codahale.metrics.Timer.Context codahaleContext;

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
        public void close() throws Exception
        {
            stop();
        }
    }
}
