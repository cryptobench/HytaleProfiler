package com.hytaleprofiler.data;

import java.util.Map;

/**
 * Data class holding JVM memory and GC statistics.
 */
public class JVMData {
    private final long heapUsed;
    private final long heapMax;
    private final long nonHeapUsed;
    private final int threadCount;
    private final long totalGcCount;
    private final long totalGcTimeMs;
    private final Map<String, GCStats> gcByCollector;

    public JVMData(long heapUsed, long heapMax, long nonHeapUsed, int threadCount,
                   long totalGcCount, long totalGcTimeMs, Map<String, GCStats> gcByCollector) {
        this.heapUsed = heapUsed;
        this.heapMax = heapMax;
        this.nonHeapUsed = nonHeapUsed;
        this.threadCount = threadCount;
        this.totalGcCount = totalGcCount;
        this.totalGcTimeMs = totalGcTimeMs;
        this.gcByCollector = gcByCollector;
    }

    public long getHeapUsed() {
        return heapUsed;
    }

    public long getHeapMax() {
        return heapMax;
    }

    public long getNonHeapUsed() {
        return nonHeapUsed;
    }

    public double getHeapPercentage() {
        if (heapMax <= 0) return 0;
        return ((double) heapUsed / heapMax) * 100.0;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getTotalGcCount() {
        return totalGcCount;
    }

    public long getTotalGcTimeMs() {
        return totalGcTimeMs;
    }

    public Map<String, GCStats> getGcByCollector() {
        return gcByCollector;
    }

    /**
     * Statistics for a single garbage collector.
     */
    public static class GCStats {
        private final long count;
        private final long timeMs;

        public GCStats(long count, long timeMs) {
            this.count = count;
            this.timeMs = timeMs;
        }

        public long getCount() {
            return count;
        }

        public long getTimeMs() {
            return timeMs;
        }
    }
}
