package org.commonjava.o11yphant.metrics.impl;

import org.commonjava.o11yphant.api.Snapshot;
import org.commonjava.o11yphant.api.Timer;

import java.util.concurrent.TimeUnit;

public class O11Timer
                implements Timer
{
    private com.codahale.metrics.Timer codahaleTimer;

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
    public void update( long duration, TimeUnit nanoseconds )
    {
        codahaleTimer.update( duration, nanoseconds );
    }

    @Override
    public Snapshot getSnapshot()
    {
        final com.codahale.metrics.Snapshot codehaleSnapshot = codahaleTimer.getSnapshot();
        return new Snapshot()
        {
            @Override
            public double getValue( double var1 )
            {
                return codehaleSnapshot.getValue( var1 );
            }

            @Override
            public long[] getValues()
            {
                return codehaleSnapshot.getValues();
            }

            @Override
            public int size()
            {
                return codehaleSnapshot.size();
            }

            @Override
            public long getMax()
            {
                return codehaleSnapshot.getMax();
            }

            @Override
            public double getMean()
            {
                return codehaleSnapshot.getMean();
            }

            @Override
            public long getMin()
            {
                return codehaleSnapshot.getMin();
            }

            @Override
            public double getStdDev()
            {
                return codehaleSnapshot.getStdDev();
            }
        };
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
