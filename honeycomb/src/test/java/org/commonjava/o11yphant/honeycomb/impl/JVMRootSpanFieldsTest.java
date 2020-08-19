package org.commonjava.o11yphant.honeycomb.impl;

import org.commonjava.o11yphant.metrics.jvm.JVMInstrumentation;
import org.junit.Test;

import java.util.Map;

public class JVMRootSpanFieldsTest
{
    @Test
    public void run()
    {
        JVMRootSpanFields jvmRootSpanFields = new JVMRootSpanFields( new JVMInstrumentation( null ) );
        Map<String, Object> map = jvmRootSpanFields.get();
        System.out.println( map ); // print something like below,
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
