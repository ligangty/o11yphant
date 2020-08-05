/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/o11yphant)
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
package org.commonjava.o11yphant.metrics.util;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.ClassUtils;
import org.commonjava.o11yphant.metrics.MetricsConstants;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.commons.lang.StringUtils.isBlank;

public class NameUtils
{
    private static final int DEFAULT_LEN = 40;

    public static String getAbbreviatedName( Class cls )
    {
        return ClassUtils.getAbbreviatedName( cls, DEFAULT_LEN );
    }

    public static String name( Class<?> klass, String... names )
    {
        return name( klass.getName(), names );
    }

    public static String name( String name, String... names )
    {
        StringBuilder builder = new StringBuilder();
        append( builder, name );
        if ( names != null )
        {
            String[] var3 = names;
            int var4 = names.length;

            for ( int var5 = 0; var5 < var4; ++var5 )
            {
                String s = var3[var5];
                append( builder, s );
            }
        }

        return builder.toString();
    }

    private static void append( StringBuilder builder, String part )
    {
        if ( part != null && !part.isEmpty() )
        {
            if ( builder.length() > 0 )
            {
                builder.append( '.' );
            }

            builder.append( part );
        }

    }

    /**
     * Get default metric name. Use abbreviated package name, e.g., foo.bar.ClassA.methodB -> f.b.ClassA.methodB
     */
    public static String getDefaultName( Class<?> declaringClass, String method )
    {
        // minimum len 1 shortens the package name and keeps class name
        String cls = ClassUtils.getAbbreviatedName( declaringClass.getName(), 1 );
        return MetricRegistry.name( cls, method );
    }

    /**
     * Get default metric name. Use abbreviated package name, e.g., foo.bar.ClassA.methodB -> f.b.ClassA.methodB
     */
    public static String getDefaultName( String declaringClass, String method )
    {
        // minimum len 1 shortens the package name and keeps class name
        String cls = ClassUtils.getAbbreviatedName( declaringClass, 1 );
        return MetricRegistry.name( cls, method );
    }

    /**
     * Get the metric fullname with no default value.
     * @param nameParts user specified name parts
     */
    public static String getSupername( String nodePrefix, String... nameParts )
    {
        return MetricRegistry.name( nodePrefix, nameParts );
    }

    /**
     * Get the metric fullname.
     * @param name user specified name
     * @param defaultName 'class name + method name', not null.
     */
    public static String getName( String nodePrefix, String name, String defaultName, String... suffix )
    {
        if ( isBlank( name ) || name.equals( MetricsConstants.DEFAULT ) )
        {
            name = defaultName;
        }
        return MetricRegistry.name( MetricRegistry.name( nodePrefix, name ), suffix );
    }
}
