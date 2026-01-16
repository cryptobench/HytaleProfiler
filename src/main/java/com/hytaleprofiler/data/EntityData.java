package com.hytaleprofiler.data;

import java.util.Map;

/**
 * Data class holding entity count information.
 */
public class EntityData {
    private final int totalEntityCount;
    private final int archetypeCount;
    private final Map<String, Integer> countsByType;

    public EntityData(int totalEntityCount, int archetypeCount, Map<String, Integer> countsByType) {
        this.totalEntityCount = totalEntityCount;
        this.archetypeCount = archetypeCount;
        this.countsByType = countsByType;
    }

    public int getTotalEntityCount() {
        return totalEntityCount;
    }

    public int getArchetypeCount() {
        return archetypeCount;
    }

    public Map<String, Integer> getCountsByType() {
        return countsByType;
    }
}
