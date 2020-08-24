package org.commonjava.o11yphant.honeycomb.impl;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;
import org.commonjava.o11yphant.honeycomb.RootSpanFields;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

/**
 * Allow an application to register a new RootSpanFields implementation for any Cassandra connection pool it sets up.
 */
public abstract class CassandraConnectionRootSpanFields
                implements RootSpanFields
{
    /**
     * Get cassandra sessions, each with a name (usually it is the keyspace name).
     */
    protected abstract Map<String, Session> getSessions();

    private Map<String, Session> sessions;

    public CassandraConnectionRootSpanFields()
    {
        this.sessions = getSessions();
    }

    public CassandraConnectionRootSpanFields( Map<String, Session> sessions )
    {
        this.sessions = sessions;
    }

    @Override
    public Map<String, Object> get()
    {
        if ( sessions == null || sessions.isEmpty() )
        {
            return emptyMap();
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
                ret.put( name( prefix, hostname, "totalConnections" ), total );
                ret.put( name( prefix, hostname, "openConnections" ), open );
                ret.put( name( prefix, hostname, "trashedConnections" ), trashed );
            } );
        } );
        return ret;
    }
}
