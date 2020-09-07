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
package org.commonjava.o11yphant.metrics.conf;

public class ELKConfig
{
    private long elkJVMPeriodInSeconds;

    private String elkHosts;

    private String elkIndex;

    private long elkPeriodInSeconds;

    private String elkPrefix;

    private long elkHealthCheckPeriodInSeconds;

    public long getElkJVMPeriodInSeconds()
    {
        return elkJVMPeriodInSeconds;
    }

    public void setElkJVMPeriodInSeconds( long elkJVMPeriodInSeconds )
    {
        this.elkJVMPeriodInSeconds = elkJVMPeriodInSeconds;
    }

    public String getElkHosts()
    {
        return elkHosts;
    }

    public void setElkHosts( String elkHosts )
    {
        this.elkHosts = elkHosts;
    }

    public String getElkIndex()
    {
        return elkIndex;
    }

    public void setElkIndex( String elkIndex )
    {
        this.elkIndex = elkIndex;
    }

    public long getElkPeriodInSeconds()
    {
        return elkPeriodInSeconds;
    }

    public void setElkPeriodInSeconds( long elkPeriodInSeconds )
    {
        this.elkPeriodInSeconds = elkPeriodInSeconds;
    }

    public String getElkPrefix()
    {
        return elkPrefix;
    }

    public void setElkPrefix( String elkPrefix )
    {
        this.elkPrefix = elkPrefix;
    }

    public long getElkHealthCheckPeriodInSeconds()
    {
        return elkHealthCheckPeriodInSeconds;
    }

    public void setElkHealthCheckPeriodInSeconds( long elkHealthCheckPeriodInSeconds )
    {
        this.elkHealthCheckPeriodInSeconds = elkHealthCheckPeriodInSeconds;
    }
}
