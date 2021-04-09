package org.commonjava.o11yphant.trace;

import org.commonjava.o11yphant.trace.spi.RootSpanFields;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public final class CDIRootSpanDecorator extends RootSpanDecorator
{
    @Inject
    private Instance<RootSpanFields> rootSpanFieldsInstance;

    @PostConstruct
    public final void init()
    {
        if ( !rootSpanFieldsInstance.isUnsatisfied() )
        {
            List<RootSpanFields> rsf = new ArrayList<>();
            rootSpanFieldsInstance.forEach( ( fields ) -> rsf.add( fields ) );
            registerRootSpanFields( rsf );
        }
    }
}
