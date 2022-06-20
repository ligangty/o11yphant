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

import org.commonjava.o11yphant.metrics.api.Snapshot;

public class O11Snapshot
                implements Snapshot
{
    private com.codahale.metrics.Snapshot codehaleSnapshot;

    public O11Snapshot( com.codahale.metrics.Snapshot codehaleSnapshot )
    {
        this.codehaleSnapshot = codehaleSnapshot;
    }

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
}
