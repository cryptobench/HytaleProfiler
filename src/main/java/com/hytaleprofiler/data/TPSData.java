package com.hytaleprofiler.data;

/**
 * Data class holding TPS and tick timing information.
 */
public class TPSData {
    private final double tps;
    private final double avgTickMs;
    private final double minTickMs;
    private final double maxTickMs;
    private final double[] tickHistory;

    public TPSData(double tps, double avgTickMs, double minTickMs, double maxTickMs, double[] tickHistory) {
        this.tps = tps;
        this.avgTickMs = avgTickMs;
        this.minTickMs = minTickMs;
        this.maxTickMs = maxTickMs;
        this.tickHistory = tickHistory;
    }

    public double getTps() {
        return tps;
    }

    public double getTpsPercentage() {
        return Math.min(100.0, (tps / 20.0) * 100.0);
    }

    public double getAvgTickMs() {
        return avgTickMs;
    }

    public double getMinTickMs() {
        return minTickMs;
    }

    public double getMaxTickMs() {
        return maxTickMs;
    }

    public double[] getTickHistory() {
        return tickHistory;
    }

    public boolean isHealthy() {
        return tps >= 19.0;
    }

    public boolean isWarning() {
        return tps >= 15.0 && tps < 19.0;
    }

    public boolean isCritical() {
        return tps < 15.0;
    }
}
