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

import org.commonjava.o11yphant.metrics.jvm.JVMInstrumentation;
import org.junit.Test;

public class JVMSpanFieldsInjectorTest
{
    @Test
    public void run()
    {
        JVMSpanFieldsInjector jvmRootSpanFields = new JVMSpanFieldsInjector( new JVMInstrumentation( null ) );
        MockSpan span = new MockSpan();
        jvmRootSpanFields.decorateSpanAtStart( span );
        jvmRootSpanFields.decorateSpanAtClose( span );
        System.out.println( span.getFields() ); // print something like below,
        /*
        jvm.memory.heap.used=15177840,
        jvm.memory.non-heap.init=2555904,
        jvm.memory.heap.max=1786249216,
        jvm.memory.non-heap.used=9485288,
        jvm.memory.heap.usage=0.008497045017037533,
        jvm.memory.non-heap.committed=10682368,
        jvm.memory.heap.init=125829120,
        jvm.memory.non-heap.usage=-9501416.0,
        jvm.memory.non-heap.max=-1,
        jvm.memory.total.used=24666744,
        jvm.memory.total.init=128385024,
        jvm.memory.heap.committed=120586240,
        jvm.memory.total.max=1786249215,
        jvm.memory.total.committed=131268608,
        jvm.threads.deadlock.count=0,
        jvm.threads.daemon.count=4,
        jvm.threads.new.count=0,
        jvm.threads.timed_waiting.count=0,
        jvm.threads.blocked.count=0,
        jvm.threads.count=5,
        jvm.threads.terminated.count=0,
        jvm.threads.waiting.count=2,
        jvm.threads.runnable.count=3
        */
    }
}
