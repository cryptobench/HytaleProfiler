package com.hytaleprofiler.data;

/**
 * Data class holding profiling information for an ECS system.
 */
public class SystemProfile implements Comparable<SystemProfile> {
    private final String name;
    private final String className;
    private final String modName;
    private final double avgMs;
    private final double minMs;
    private final double maxMs;
    private final int sampleCount;

    public SystemProfile(String name, String className, String modName,
                         double avgMs, double minMs, double maxMs, int sampleCount) {
        this.name = name;
        this.className = className;
        this.modName = modName;
        this.avgMs = avgMs;
        this.minMs = minMs;
        this.maxMs = maxMs;
        this.sampleCount = sampleCount;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getModName() {
        return modName;
    }

    public double getAvgMs() {
        return avgMs;
    }

    public double getMinMs() {
        return minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getPercentageOf(double totalMs) {
        if (totalMs <= 0) return 0;
        return (avgMs / totalMs) * 100.0;
    }

    @Override
    public int compareTo(SystemProfile other) {
        // Sort by avgMs descending (slowest first)
        return Double.compare(other.avgMs, this.avgMs);
    }
}
