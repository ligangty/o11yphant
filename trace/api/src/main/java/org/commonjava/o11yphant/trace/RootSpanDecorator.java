package org.commonjava.o11yphant.trace;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.RootSpanFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RootSpanDecorator
{
    private List<RootSpanFields> rootSpanFields = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger( getClass().getName());

    protected RootSpanDecorator()
    {
    }

    public RootSpanDecorator( List<RootSpanFields> rootSpanFields )
    {
        this.rootSpanFields = rootSpanFields;
    }

    protected void registerRootSpanFields( List<RootSpanFields> rootSpanFields )
    {
        this.rootSpanFields = rootSpanFields;
    }

    public final void decorate( SpanAdapter span )
    {
        rootSpanFields.forEach( rootSpanFields -> {
            Map<String, Object> fields = rootSpanFields.get();
            if ( fields != null )
            {
                fields.forEach( ( k, v ) -> span.addField( k, v ) );
            }
            logger.debug( "Add root span fields for: {}, fields: {}", rootSpanFields, fields );
        } );
    }
}
