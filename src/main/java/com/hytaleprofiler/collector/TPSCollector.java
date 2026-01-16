package com.hytaleprofiler.collector;

import com.hytaleprofiler.data.TPSData;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Collects TPS and tick timing data from the world.
 */
public class TPSCollector {

    private static final double TARGET_TPS = 20.0;
    private static final double NANOS_PER_MS = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    /**
     * Collect TPS data from the given world.
     */
    public TPSData collect(World world) {
        HistoricMetric tickMetric = world.getBufferedTickLengthMetricSet();

        if (tickMetric == null) {
            return new TPSData(TARGET_TPS, 50.0, 50.0, 50.0, new double[0]);
        }

        double avgNs = tickMetric.getAverage(0);
        double minNs = tickMetric.calculateMin(0);
        double maxNs = tickMetric.calculateMax(0);

        // Convert to milliseconds
        double avgMs = avgNs / NANOS_PER_MS;
        double minMs = minNs / NANOS_PER_MS;
        double maxMs = maxNs / NANOS_PER_MS;

        // Calculate actual TPS from average tick time
        double tps = avgNs > 0 ? NANOS_PER_SECOND / avgNs : TARGET_TPS;
        tps = Math.min(tps, TARGET_TPS); // Cap at 20

        // Get history if available
        double[] history = getTickHistory(tickMetric);

        return new TPSData(tps, avgMs, minMs, maxMs, history);
    }

    private double[] getTickHistory(HistoricMetric metric) {
        try {
            long[] values = metric.getAllValues();
            if (values == null || values.length == 0) {
                return new double[0];
            }

            double[] history = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                history[i] = values[i] / NANOS_PER_MS;
            }
            return history;
        } catch (Exception e) {
            return new double[0];
        }
    }
}
