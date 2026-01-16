package com.hytaleprofiler.data;

/**
 * Data class holding aggregated profiling information for a mod.
 */
public class ModProfile implements Comparable<ModProfile> {
    private final String modName;
    private final double totalMs;
    private final int systemCount;

    public ModProfile(String modName, double totalMs, int systemCount) {
        this.modName = modName;
        this.totalMs = totalMs;
        this.systemCount = systemCount;
    }

    public String getModName() {
        return modName;
    }

    public double getTotalMs() {
        return totalMs;
    }

    public int getSystemCount() {
        return systemCount;
    }

    public double getPercentageOf(double totalTickMs) {
        if (totalTickMs <= 0) return 0;
        return (totalMs / totalTickMs) * 100.0;
    }

    @Override
    public int compareTo(ModProfile other) {
        // Sort by totalMs descending (slowest first)
        return Double.compare(other.totalMs, this.totalMs);
    }
}
