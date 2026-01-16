package com.hytaleprofiler.collector;

import com.hytaleprofiler.data.JVMData;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects JVM memory and garbage collection statistics.
 */
public class JVMMetricsCollector {

    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final List<GarbageCollectorMXBean> gcBeans;

    public JVMMetricsCollector() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * Collect current JVM metrics.
     */
    public JVMData collect() {
        // Memory stats
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        long heapUsed = heapUsage.getUsed();
        long heapMax = heapUsage.getMax();
        long nonHeapUsed = nonHeapUsage.getUsed();

        // Thread count
        int threadCount = threadMXBean.getThreadCount();

        // GC stats
        long totalGcCount = 0;
        long totalGcTimeMs = 0;
        Map<String, JVMData.GCStats> gcByCollector = new HashMap<>();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName();
            long count = gcBean.getCollectionCount();
            long timeMs = gcBean.getCollectionTime();

            if (count >= 0) {
                totalGcCount += count;
            }
            if (timeMs >= 0) {
                totalGcTimeMs += timeMs;
            }

            gcByCollector.put(name, new JVMData.GCStats(
                count >= 0 ? count : 0,
                timeMs >= 0 ? timeMs : 0
            ));
        }

        return new JVMData(
            heapUsed, heapMax, nonHeapUsed, threadCount,
            totalGcCount, totalGcTimeMs, gcByCollector
        );
    }

    /**
     * Trigger garbage collection.
     */
    public void triggerGC() {
        System.gc();
    }
}
