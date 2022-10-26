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
package org.commonjava.o11yphant.trace.spi;

import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;

import java.util.Optional;

/**
 * These are fields available for injection when the span is finally closed. It is designed to work with a close-counting
 * span adapter, such that the number of close() calls has to match the number of decorators. When the close call count
 * does match the decorator count, everything gets closed down.
 *
 * The purpose is to allow something like a REST request to return when a transfer stream is still pending. In that
 * case, you want to add some final span information when the transfer completes, or else you could skew some measurements
 * compared to what the client actually experiences. If we're streaming a file back to a caller, we don't want the
 * servlet to terminate the span...we want the transfer thread to do it.
 *
 * @see org.commonjava.o11yphant.trace.TraceManager#addCloseBlockingDecorator(Optional, CloseBlockingDecorator)
 */
public interface CloseBlockingDecorator
{
    void decorateSpanAtClose( SpanAdapter span );
}
