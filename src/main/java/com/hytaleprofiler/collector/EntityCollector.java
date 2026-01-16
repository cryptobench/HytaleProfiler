package com.hytaleprofiler.collector;

import com.hytaleprofiler.data.EntityData;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.metric.ArchetypeChunkData;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.TreeMap;

/**
 * Collects entity count data from the world.
 */
public class EntityCollector {

    /**
     * Collect entity data from the given world.
     */
    public EntityData collect(World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        int totalCount = 0;
        Map<String, Integer> countsByType = new TreeMap<>();

        try {
            ArchetypeChunkData[] chunks = store.collectArchetypeChunkData();

            if (chunks != null) {
                for (ArchetypeChunkData chunk : chunks) {
                    if (chunk == null) continue;

                    int count = chunk.getEntityCount();
                    totalCount += count;

                    // Build a signature from component types
                    String signature = buildSignature(chunk);
                    countsByType.merge(signature, count, Integer::sum);
                }
            }
        } catch (Exception e) {
            // If archetype data isn't available, try to get basic count
            try {
                totalCount = store.getEntityCount();
            } catch (Exception ignored) {
            }
        }

        return new EntityData(totalCount, countsByType.size(), countsByType);
    }

    private String buildSignature(ArchetypeChunkData chunk) {
        try {
            String[] types = chunk.getComponentTypes();
            if (types == null || types.length == 0) {
                return "Unknown";
            }

            // Use the most identifying component (often the first non-common one)
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String type : types) {
                if (type == null) continue;
                if (isIdentifyingComponent(type)) {
                    if (count > 0) sb.append("+");
                    sb.append(simplifyComponentName(type));
                    count++;
                    if (count >= 3) break; // Limit to 3 components for readability
                }
            }
            return sb.length() > 0 ? sb.toString() : "Entity";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private boolean isIdentifyingComponent(String name) {
        // Filter out very common/internal components
        String lower = name.toLowerCase();
        return !lower.contains("transform")
            && !lower.contains("velocity")
            && !lower.contains("position")
            && !lower.contains("rotation");
    }

    private String simplifyComponentName(String fullName) {
        // Extract simple name from fully qualified
        int lastDot = fullName.lastIndexOf('.');
        String simple = lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;

        // Remove common suffixes
        if (simple.endsWith("Component")) {
            simple = simple.substring(0, simple.length() - 9);
        }
        return simple;
    }
}
