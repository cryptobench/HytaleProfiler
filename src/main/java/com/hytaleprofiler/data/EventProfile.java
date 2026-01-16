package com.hytaleprofiler.data;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Data class holding profiling information for an event type.
 * Thread-safe for concurrent updates.
 */
public class EventProfile implements Comparable<EventProfile> {
    private final String eventName;
    private final String eventClassName;
    private final LongAdder totalTimeNanos = new LongAdder();
    private final LongAdder callCount = new LongAdder();
    private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTimeNanos = new AtomicLong(0);

    public EventProfile(String eventName, String eventClassName) {
        this.eventName = eventName;
        this.eventClassName = eventClassName;
    }

    /**
     * Record a single event execution.
     */
    public void record(long durationNanos) {
        totalTimeNanos.add(durationNanos);
        callCount.increment();

        // Update min
        long currentMin;
        do {
            currentMin = minTimeNanos.get();
            if (durationNanos >= currentMin) break;
        } while (!minTimeNanos.compareAndSet(currentMin, durationNanos));

        // Update max
        long currentMax;
        do {
            currentMax = maxTimeNanos.get();
            if (durationNanos <= currentMax) break;
        } while (!maxTimeNanos.compareAndSet(currentMax, durationNanos));
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    public long getTotalTimeNanos() {
        return totalTimeNanos.sum();
    }

    public double getTotalTimeMs() {
        return totalTimeNanos.sum() / 1_000_000.0;
    }

    public long getCallCount() {
        return callCount.sum();
    }

    public double getAvgTimeMs() {
        long count = callCount.sum();
        if (count == 0) return 0;
        return (totalTimeNanos.sum() / (double) count) / 1_000_000.0;
    }

    public double getMinTimeMs() {
        long min = minTimeNanos.get();
        if (min == Long.MAX_VALUE) return 0;
        return min / 1_000_000.0;
    }

    public double getMaxTimeMs() {
        return maxTimeNanos.get() / 1_000_000.0;
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        totalTimeNanos.reset();
        callCount.reset();
        minTimeNanos.set(Long.MAX_VALUE);
        maxTimeNanos.set(0);
    }

    @Override
    public int compareTo(EventProfile other) {
        // Sort by total time descending
        return Long.compare(other.getTotalTimeNanos(), this.getTotalTimeNanos());
    }
}
