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
package org.commonjava.o11yphant.honeycomb;

public interface HoneycombConfiguration
{
    /**
     * For debug. Print to console if true.
     */
    default boolean isConsoleTransport()
    {
        return false;
    }

    /**
     * Honeycomb writekey.
     */
    String getWriteKey();

    /**
     * Honeycomb dataset name to push data to.
     */
    String getDataset();

}
