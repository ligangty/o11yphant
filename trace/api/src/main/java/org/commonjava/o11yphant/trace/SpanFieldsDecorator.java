package org.commonjava.o11yphant.trace;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpanFieldsDecorator
{
    private List<SpanFieldsInjector> spanFieldInjectors = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger( getClass().getName());

    protected SpanFieldsDecorator()
    {
    }

    public SpanFieldsDecorator( List<SpanFieldsInjector> spanFieldInjectors )
    {
        this.spanFieldInjectors = spanFieldInjectors;
    }

    protected void registerRootSpanFields( List<SpanFieldsInjector> spanFieldInjectors )
    {
        this.spanFieldInjectors = spanFieldInjectors;
    }

    public final void decorate( SpanAdapter span )
    {
        spanFieldInjectors.forEach( injectSpanFields -> {
            Map<String, Object> fields = injectSpanFields.get();
            if ( fields != null )
            {
                fields.forEach( span::addField );
            }
            logger.debug( "Add injected span fields for: {}, fields: {}", injectSpanFields, fields );
        } );
    }
}
