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
package org.commonjava.o11yphant.trace.impl;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

/**
 * Allow an application to register a new RootSpanFields implementation for any Cassandra connection pool it sets up.
 */
public abstract class CassandraConnectionSpanFieldsInjector
                implements SpanFieldsInjector
{
    /**
     * Get cassandra sessions, each with a name (usually it is the keyspace name).
     */
    protected abstract Map<String, Session> getSessions();

    @Override
    public void decorateSpanAtClose( SpanAdapter span )
    {
        final Map<String, Session> sessions = getSessions();
        if ( sessions == null || sessions.isEmpty() )
        {
            return;
        }

        Map<String, Object> ret = new HashMap<>();
        sessions.forEach( ( sessionName, session ) -> {
            Session.State st = session.getState(); // a snapshot of the state of this session
            Collection<Host> hosts = st.getConnectedHosts();
            hosts.forEach( host -> {
                String hostname = host.getAddress().getHostName();
                int open = st.getOpenConnections( host );
                int trashed = st.getTrashedConnections( host );
                int total = open + trashed;
                String prefix = name( "cassandra", sessionName );
                span.addField( name( prefix, hostname, "totalConnections" ), total );
                span.addField( name( prefix, hostname, "openConnections" ), open );
                span.addField( name( prefix, hostname, "trashedConnections" ), trashed );
            } );
        } );
    }
}
