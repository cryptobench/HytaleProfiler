package com.hytaleprofiler.collector;

import com.hytaleprofiler.data.ModProfile;
import com.hytaleprofiler.data.SystemProfile;
import com.hytaleprofiler.util.FormatUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collects ECS system performance metrics.
 */
public class SystemMetricsCollector {

    private static final double NANOS_PER_MS = 1_000_000.0;

    /**
     * Collect system profiles from the world's entity store.
     */
    public List<SystemProfile> collectSystems(World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        HistoricMetric[] systemMetrics = store.getSystemMetrics();

        if (systemMetrics == null || systemMetrics.length == 0) {
            return Collections.emptyList();
        }

        List<SystemProfile> profiles = new ArrayList<>();

        for (int i = 0; i < systemMetrics.length; i++) {
            HistoricMetric metric = systemMetrics[i];
            if (metric == null) continue;

            // Since we can't easily get system names from the Store API,
            // use a generic name based on index
            String className = "System_" + i;
            String simpleName = "System " + i;
            String modName = "Unknown";

            double avgMs = metric.getAverage(0) / NANOS_PER_MS;
            double minMs = metric.calculateMin(0) / NANOS_PER_MS;
            double maxMs = metric.calculateMax(0) / NANOS_PER_MS;

            // Skip systems with negligible time
            if (avgMs < 0.0001) continue;

            profiles.add(new SystemProfile(
                simpleName, className, modName, avgMs, minMs, maxMs, 0
            ));
        }

        // Sort by average time descending (slowest first)
        Collections.sort(profiles);
        return profiles;
    }

    /**
     * Aggregate system profiles into mod profiles.
     */
    public List<ModProfile> collectMods(World world) {
        List<SystemProfile> systems = collectSystems(world);
        return aggregateByMod(systems);
    }

    /**
     * Aggregate a list of system profiles by mod name.
     */
    public List<ModProfile> aggregateByMod(List<SystemProfile> systems) {
        Map<String, List<SystemProfile>> byMod = systems.stream()
            .collect(Collectors.groupingBy(SystemProfile::getModName));

        List<ModProfile> modProfiles = new ArrayList<>();
        for (Map.Entry<String, List<SystemProfile>> entry : byMod.entrySet()) {
            String modName = entry.getKey();
            List<SystemProfile> modSystems = entry.getValue();

            double totalMs = modSystems.stream()
                .mapToDouble(SystemProfile::getAvgMs)
                .sum();

            modProfiles.add(new ModProfile(modName, totalMs, modSystems.size()));
        }

        // Sort by total time descending
        Collections.sort(modProfiles);
        return modProfiles;
    }

    /**
     * Get total system time in milliseconds.
     */
    public double getTotalSystemTimeMs(List<SystemProfile> systems) {
        return systems.stream()
            .mapToDouble(SystemProfile::getAvgMs)
            .sum();
    }
}
