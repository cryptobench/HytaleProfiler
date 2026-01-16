package com.hytaleprofiler.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hytaleprofiler.HytaleProfiler;
import com.hytaleprofiler.data.*;
import com.hytaleprofiler.util.FormatUtil;
import com.hytaleprofiler.collector.EventTimingCollector;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main profiler command with subcommands.
 */
public class ProfilerCommand extends AbstractPlayerCommand {

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color AQUA = new Color(85, 255, 255);

    private final HytaleProfiler plugin;

    public ProfilerCommand(HytaleProfiler plugin) {
        super("profiler", "Server profiler commands");
        this.plugin = plugin;
        setAllowsExtraArguments(true);
        requirePermission("profiler.use");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {

        // Parse arguments - everything after "/profiler "
        String input = ctx.getInputString().trim();
        String afterCommand = input.length() > 9 ? input.substring(9).trim() : "";

        if (afterCommand.isEmpty()) {
            showHelp(playerData);
            return;
        }

        // Parse subcommand and optional count argument
        String[] parts = afterCommand.split("\\s+");
        String subcommand = parts[0].toLowerCase();
        Integer count = null;
        if (parts.length > 1) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        switch (subcommand) {
            case "summary" -> showSummary(playerData, world);
            case "tps" -> showTPS(playerData, world);
            case "mods" -> showMods(playerData, world);
            case "systems" -> showSystems(playerData, world, count);
            case "top" -> showTop(playerData, world, count);
            case "events" -> showEvents(playerData, count);
            case "entities" -> showEntities(playerData, world);
            case "memory" -> showMemory(playerData);
            case "export" -> exportReport(playerData, store, playerRef, world);
            case "gc" -> triggerGC(playerData, store, playerRef);
            case "reset" -> resetMetrics(playerData, store, playerRef);
            case "help" -> showHelp(playerData);
            default -> showHelp(playerData);
        }
    }

    private void showHelp(PlayerRef playerData) {
        sendMessage(playerData, "========== HytaleProfiler ==========", GOLD);
        sendMessage(playerData, "/profiler summary    - Dashboard overview", GRAY);
        sendMessage(playerData, "/profiler tps        - Detailed TPS breakdown", GRAY);
        sendMessage(playerData, "/profiler mods       - Per-mod timing breakdown", GRAY);
        sendMessage(playerData, "/profiler systems [n]- ECS system timing (default: 10)", GRAY);
        sendMessage(playerData, "/profiler top [n]    - Top N slowest systems", GRAY);
        sendMessage(playerData, "/profiler events [n] - Event handler timing", GRAY);
        sendMessage(playerData, "/profiler entities   - Entity counts by type", GRAY);
        sendMessage(playerData, "/profiler memory     - JVM memory & GC stats", GRAY);
        sendMessage(playerData, "/profiler export     - Export full report to JSON", GRAY);
        sendMessage(playerData, "/profiler gc         - Trigger garbage collection", GRAY);
        sendMessage(playerData, "/profiler reset      - Clear metrics history", GRAY);
        sendMessage(playerData, "=====================================", GOLD);
    }

    private void showSummary(PlayerRef playerData, World world) {
        TPSData tps = plugin.getTpsCollector().collect(world);
        List<SystemProfile> systems = plugin.getSystemMetricsCollector().collectSystems(world);
        List<ModProfile> mods = plugin.getSystemMetricsCollector().aggregateByMod(systems);
        EntityData entities = plugin.getEntityCollector().collect(world);
        JVMData jvm = plugin.getJvmMetricsCollector().collect();
        EventTimingCollector eventCollector = plugin.getEventTimingCollector();

        double totalSystemMs = plugin.getSystemMetricsCollector().getTotalSystemTimeMs(systems);
        double totalEventMs = eventCollector.getTotalEventTimeMs();
        long totalEventCalls = eventCollector.getTotalEventCount();

        sendMessage(playerData, "========== Server Profiler ==========", GOLD);

        // TPS line
        Color tpsColor = tps.isHealthy() ? GREEN : (tps.isWarning() ? YELLOW : RED);
        String tpsLine = String.format("TPS: %s (%s) | Tick: %s",
            FormatUtil.formatTps(tps.getTps()),
            FormatUtil.formatPercent(tps.getTpsPercentage()),
            FormatUtil.formatMs(tps.getAvgTickMs()));
        sendMessage(playerData, tpsLine, tpsColor);

        // Memory line
        Color memColor = jvm.getHeapPercentage() > 80 ? RED : (jvm.getHeapPercentage() > 60 ? YELLOW : GREEN);
        String memLine = String.format("Memory: %s / %s (%s) | GC: %s runs",
            FormatUtil.formatBytes(jvm.getHeapUsed()),
            FormatUtil.formatBytes(jvm.getHeapMax()),
            FormatUtil.formatPercent(jvm.getHeapPercentage()),
            FormatUtil.formatCount(jvm.getTotalGcCount()));
        sendMessage(playerData, memLine, memColor);

        // Entity line
        String entityLine = String.format("Entities: %s | Archetypes: %d",
            FormatUtil.formatCount(entities.getTotalEntityCount()),
            entities.getArchetypeCount());
        sendMessage(playerData, entityLine, WHITE);

        // System count
        String systemLine = String.format("Systems: %d | Total time: %s",
            systems.size(), FormatUtil.formatMs(totalSystemMs));
        sendMessage(playerData, systemLine, WHITE);

        // Event handler timing
        String eventLine = String.format("Events: %s calls | Total time: %s",
            FormatUtil.formatCount(totalEventCalls), FormatUtil.formatMs(totalEventMs));
        sendMessage(playerData, eventLine, WHITE);

        // Top slowest systems
        if (!systems.isEmpty()) {
            sendMessage(playerData, "", WHITE);
            sendMessage(playerData, "Slowest Systems:", AQUA);
            int rank = 1;
            for (SystemProfile sys : systems) {
                if (rank > 3) break;
                String line = String.format("  %d. %s - %s (%s)",
                    rank, sys.getName(),
                    FormatUtil.formatMs(sys.getAvgMs()),
                    FormatUtil.formatPercent(sys.getPercentageOf(totalSystemMs)));
                sendMessage(playerData, line, GRAY);
                rank++;
            }
        }

        // Top slowest events (if any have been called)
        List<EventProfile> activeEvents = eventCollector.getActiveProfiles();
        if (!activeEvents.isEmpty()) {
            sendMessage(playerData, "", WHITE);
            sendMessage(playerData, "Slowest Events:", AQUA);
            int rank = 1;
            for (EventProfile event : activeEvents) {
                if (rank > 3) break;
                String line = String.format("  %d. %s - %s avg (%s calls)",
                    rank, event.getEventName(),
                    FormatUtil.formatMs(event.getAvgTimeMs()),
                    FormatUtil.formatCount(event.getCallCount()));
                sendMessage(playerData, line, GRAY);
                rank++;
            }
        }

        sendMessage(playerData, "=====================================", GOLD);
    }

    private void showTPS(PlayerRef playerData, World world) {
        TPSData tps = plugin.getTpsCollector().collect(world);

        sendMessage(playerData, "========== TPS Details ==========", GOLD);

        Color tpsColor = tps.isHealthy() ? GREEN : (tps.isWarning() ? YELLOW : RED);
        sendMessage(playerData, String.format("Current TPS: %s (%s)",
            FormatUtil.formatTps(tps.getTps()),
            FormatUtil.formatPercent(tps.getTpsPercentage())), tpsColor);

        sendMessage(playerData, "", WHITE);
        sendMessage(playerData, "Tick Timing:", AQUA);
        sendMessage(playerData, String.format("  Average: %s", FormatUtil.formatMs(tps.getAvgTickMs())), GRAY);
        sendMessage(playerData, String.format("  Minimum: %s", FormatUtil.formatMs(tps.getMinTickMs())), GRAY);
        sendMessage(playerData, String.format("  Maximum: %s", FormatUtil.formatMs(tps.getMaxTickMs())), GRAY);

        // Status indicator
        sendMessage(playerData, "", WHITE);
        String status = tps.isHealthy() ? "HEALTHY" : (tps.isWarning() ? "WARNING" : "CRITICAL");
        sendMessage(playerData, "Status: " + status, tpsColor);

        // Health bar
        String bar = FormatUtil.progressBar(tps.getTpsPercentage(), 20);
        sendMessage(playerData, bar + " " + FormatUtil.formatPercent(tps.getTpsPercentage()), tpsColor);

        sendMessage(playerData, "=================================", GOLD);
    }

    private void showMods(PlayerRef playerData, World world) {
        List<SystemProfile> systems = plugin.getSystemMetricsCollector().collectSystems(world);
        List<ModProfile> mods = plugin.getSystemMetricsCollector().aggregateByMod(systems);
        double totalMs = plugin.getSystemMetricsCollector().getTotalSystemTimeMs(systems);

        sendMessage(playerData, "=== Mod Performance Breakdown ===", GOLD);
        sendMessage(playerData, String.format("Total system time: %s", FormatUtil.formatMs(totalMs)), WHITE);
        sendMessage(playerData, "", WHITE);

        int rank = 1;
        for (ModProfile mod : mods) {
            String line = String.format("%d. %s %s (%s) [%d systems]",
                rank,
                FormatUtil.padRight(mod.getModName(), 20),
                FormatUtil.padLeft(FormatUtil.formatMs(mod.getTotalMs()), 10),
                FormatUtil.padLeft(FormatUtil.formatPercent(mod.getPercentageOf(totalMs)), 6),
                mod.getSystemCount());

            Color color = rank == 1 ? YELLOW : GRAY;
            sendMessage(playerData, line, color);
            rank++;
        }

        sendMessage(playerData, "", WHITE);
        sendMessage(playerData, "Use /profiler systems to see individual systems", GRAY);
        sendMessage(playerData, "=================================", GOLD);
    }

    private void showSystems(PlayerRef playerData, World world, Integer count) {
        int limit = count != null && count > 0 ? count : 10;
        List<SystemProfile> systems = plugin.getSystemMetricsCollector().collectSystems(world);
        double totalMs = plugin.getSystemMetricsCollector().getTotalSystemTimeMs(systems);

        sendMessage(playerData, "=== ECS System Timing ===", GOLD);
        sendMessage(playerData, String.format("Total: %s | Systems: %d", FormatUtil.formatMs(totalMs), systems.size()), WHITE);
        sendMessage(playerData, "", WHITE);

        int rank = 1;
        for (SystemProfile sys : systems) {
            if (rank > limit) break;

            String line = String.format("%2d. %s %s (%s)",
                rank,
                FormatUtil.padRight(sys.getName(), 25),
                FormatUtil.padLeft(FormatUtil.formatMs(sys.getAvgMs()), 10),
                FormatUtil.padLeft(FormatUtil.formatPercent(sys.getPercentageOf(totalMs)), 6));

            Color color = rank <= 3 ? YELLOW : GRAY;
            sendMessage(playerData, line, color);
            rank++;
        }

        if (systems.size() > limit) {
            sendMessage(playerData, String.format("... and %d more systems", systems.size() - limit), GRAY);
        }

        sendMessage(playerData, "=========================", GOLD);
    }

    private void showTop(PlayerRef playerData, World world, Integer count) {
        int limit = count != null && count > 0 ? count : 5;
        List<SystemProfile> systems = plugin.getSystemMetricsCollector().collectSystems(world);
        double totalMs = plugin.getSystemMetricsCollector().getTotalSystemTimeMs(systems);

        sendMessage(playerData, "=== Top " + limit + " Slowest Systems ===", GOLD);

        int rank = 1;
        for (SystemProfile sys : systems) {
            if (rank > limit) break;

            sendMessage(playerData, String.format("#%d %s", rank, sys.getName()), YELLOW);
            sendMessage(playerData, String.format("   Avg: %s | Min: %s | Max: %s",
                FormatUtil.formatMs(sys.getAvgMs()),
                FormatUtil.formatMs(sys.getMinMs()),
                FormatUtil.formatMs(sys.getMaxMs())), GRAY);
            sendMessage(playerData, String.format("   %s of tick",
                FormatUtil.formatPercent(sys.getPercentageOf(totalMs))), GRAY);

            rank++;
        }

        sendMessage(playerData, "==============================", GOLD);
    }

    private void showEvents(PlayerRef playerData, Integer count) {
        int limit = count != null && count > 0 ? count : 10;
        EventTimingCollector eventCollector = plugin.getEventTimingCollector();
        List<EventProfile> events = eventCollector.getProfiles();
        List<EventProfile> activeEvents = eventCollector.getActiveProfiles();
        double totalMs = eventCollector.getTotalEventTimeMs();
        long totalCalls = eventCollector.getTotalEventCount();

        sendMessage(playerData, "=== Event Handler Timing ===", GOLD);
        sendMessage(playerData, String.format("Total calls: %s | Total time: %s",
            FormatUtil.formatCount(totalCalls), FormatUtil.formatMs(totalMs)), WHITE);
        sendMessage(playerData, String.format("Tracked events: %d | Active: %d",
            events.size(), activeEvents.size()), WHITE);
        sendMessage(playerData, "", WHITE);

        if (activeEvents.isEmpty()) {
            sendMessage(playerData, "No events have been triggered yet.", GRAY);
            sendMessage(playerData, "Play on the server to generate event data.", GRAY);
        } else {
            int rank = 1;
            for (EventProfile event : activeEvents) {
                if (rank > limit) break;

                // Color based on avg time - highlight slow events
                Color eventColor = event.getAvgTimeMs() > 1.0 ? YELLOW :
                    (event.getAvgTimeMs() > 0.1 ? WHITE : GRAY);

                String line = String.format("%2d. %s",
                    rank, FormatUtil.padRight(event.getEventName(), 20));
                sendMessage(playerData, line, eventColor);

                sendMessage(playerData, String.format("    Calls: %s | Total: %s | Avg: %s",
                    FormatUtil.formatCount(event.getCallCount()),
                    FormatUtil.formatMs(event.getTotalTimeMs()),
                    FormatUtil.formatMs(event.getAvgTimeMs())), GRAY);

                sendMessage(playerData, String.format("    Min: %s | Max: %s",
                    FormatUtil.formatMs(event.getMinTimeMs()),
                    FormatUtil.formatMs(event.getMaxTimeMs())), GRAY);

                rank++;
            }

            if (activeEvents.size() > limit) {
                sendMessage(playerData, String.format("... and %d more events",
                    activeEvents.size() - limit), GRAY);
            }
        }

        sendMessage(playerData, "", WHITE);
        sendMessage(playerData, "Note: Times include all handlers for each event type.", GRAY);
        sendMessage(playerData, "============================", GOLD);
    }

    private void showEntities(PlayerRef playerData, World world) {
        EntityData entities = plugin.getEntityCollector().collect(world);

        sendMessage(playerData, "=== Entity Breakdown ===", GOLD);
        sendMessage(playerData, String.format("Total Entities: %s",
            FormatUtil.formatCount(entities.getTotalEntityCount())), WHITE);
        sendMessage(playerData, String.format("Archetypes: %d", entities.getArchetypeCount()), WHITE);
        sendMessage(playerData, "", WHITE);

        Map<String, Integer> counts = entities.getCountsByType();
        if (counts.isEmpty()) {
            sendMessage(playerData, "No detailed archetype data available.", GRAY);
        } else {
            // Sort by count descending
            counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(15)
                .forEach(entry -> {
                    String line = String.format("  %s: %s",
                        FormatUtil.padRight(entry.getKey(), 30),
                        FormatUtil.formatCount(entry.getValue()));
                    sendMessage(playerData, line, GRAY);
                });

            if (counts.size() > 15) {
                sendMessage(playerData, String.format("... and %d more types", counts.size() - 15), GRAY);
            }
        }

        sendMessage(playerData, "========================", GOLD);
    }

    private void showMemory(PlayerRef playerData) {
        JVMData jvm = plugin.getJvmMetricsCollector().collect();

        sendMessage(playerData, "=== JVM Memory Stats ===", GOLD);

        // Heap memory
        Color heapColor = jvm.getHeapPercentage() > 80 ? RED : (jvm.getHeapPercentage() > 60 ? YELLOW : GREEN);
        sendMessage(playerData, "Heap Memory:", AQUA);
        sendMessage(playerData, String.format("  Used: %s / %s (%s)",
            FormatUtil.formatBytes(jvm.getHeapUsed()),
            FormatUtil.formatBytes(jvm.getHeapMax()),
            FormatUtil.formatPercent(jvm.getHeapPercentage())), heapColor);

        String heapBar = FormatUtil.progressBar(jvm.getHeapPercentage(), 20);
        sendMessage(playerData, "  " + heapBar, heapColor);

        // Non-heap memory
        sendMessage(playerData, "", WHITE);
        sendMessage(playerData, String.format("Non-Heap: %s", FormatUtil.formatBytes(jvm.getNonHeapUsed())), GRAY);
        sendMessage(playerData, String.format("Threads: %d", jvm.getThreadCount()), GRAY);

        // GC stats
        sendMessage(playerData, "", WHITE);
        sendMessage(playerData, "Garbage Collection:", AQUA);
        sendMessage(playerData, String.format("  Total runs: %s", FormatUtil.formatCount(jvm.getTotalGcCount())), GRAY);
        sendMessage(playerData, String.format("  Total time: %dms", jvm.getTotalGcTimeMs()), GRAY);

        // Per-collector breakdown
        Map<String, JVMData.GCStats> gcStats = jvm.getGcByCollector();
        for (Map.Entry<String, JVMData.GCStats> entry : gcStats.entrySet()) {
            JVMData.GCStats stats = entry.getValue();
            sendMessage(playerData, String.format("  %s: %s runs, %dms",
                entry.getKey(),
                FormatUtil.formatCount(stats.getCount()),
                stats.getTimeMs()), GRAY);
        }

        sendMessage(playerData, "========================", GOLD);
    }

    private void exportReport(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (!player.hasPermission("profiler.admin")) {
            sendMessage(playerData, "You need profiler.admin permission to export reports.", RED);
            return;
        }

        try {
            // Collect all data
            TPSData tps = plugin.getTpsCollector().collect(world);
            List<SystemProfile> systems = plugin.getSystemMetricsCollector().collectSystems(world);
            List<ModProfile> mods = plugin.getSystemMetricsCollector().aggregateByMod(systems);
            EntityData entities = plugin.getEntityCollector().collect(world);
            JVMData jvm = plugin.getJvmMetricsCollector().collect();

            // Build report object
            Map<String, Object> report = new HashMap<>();
            report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // TPS section
            Map<String, Object> tpsSection = new HashMap<>();
            tpsSection.put("current", tps.getTps());
            tpsSection.put("percentage", tps.getTpsPercentage());
            tpsSection.put("avgTickMs", tps.getAvgTickMs());
            tpsSection.put("minTickMs", tps.getMinTickMs());
            tpsSection.put("maxTickMs", tps.getMaxTickMs());
            report.put("tps", tpsSection);

            // Mods section
            report.put("mods", mods.stream().map(m -> {
                Map<String, Object> modMap = new HashMap<>();
                modMap.put("name", m.getModName());
                modMap.put("totalMs", m.getTotalMs());
                modMap.put("systemCount", m.getSystemCount());
                return modMap;
            }).toList());

            // Systems section
            report.put("systems", systems.stream().map(s -> {
                Map<String, Object> sysMap = new HashMap<>();
                sysMap.put("name", s.getName());
                sysMap.put("className", s.getClassName());
                sysMap.put("modName", s.getModName());
                sysMap.put("avgMs", s.getAvgMs());
                sysMap.put("minMs", s.getMinMs());
                sysMap.put("maxMs", s.getMaxMs());
                return sysMap;
            }).toList());

            // Entities section
            Map<String, Object> entSection = new HashMap<>();
            entSection.put("total", entities.getTotalEntityCount());
            entSection.put("archetypes", entities.getArchetypeCount());
            entSection.put("byType", entities.getCountsByType());
            report.put("entities", entSection);

            // JVM section
            Map<String, Object> jvmSection = new HashMap<>();
            jvmSection.put("heapUsed", jvm.getHeapUsed());
            jvmSection.put("heapMax", jvm.getHeapMax());
            jvmSection.put("heapPercentage", jvm.getHeapPercentage());
            jvmSection.put("nonHeapUsed", jvm.getNonHeapUsed());
            jvmSection.put("threadCount", jvm.getThreadCount());
            jvmSection.put("totalGcCount", jvm.getTotalGcCount());
            jvmSection.put("totalGcTimeMs", jvm.getTotalGcTimeMs());
            report.put("jvm", jvmSection);

            // Events section
            EventTimingCollector eventCollector = plugin.getEventTimingCollector();
            List<EventProfile> eventProfiles = eventCollector.getActiveProfiles();
            Map<String, Object> eventsSection = new HashMap<>();
            eventsSection.put("totalCalls", eventCollector.getTotalEventCount());
            eventsSection.put("totalTimeMs", eventCollector.getTotalEventTimeMs());
            eventsSection.put("events", eventProfiles.stream().map(e -> {
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("name", e.getEventName());
                eventMap.put("className", e.getEventClassName());
                eventMap.put("callCount", e.getCallCount());
                eventMap.put("totalTimeMs", e.getTotalTimeMs());
                eventMap.put("avgTimeMs", e.getAvgTimeMs());
                eventMap.put("minTimeMs", e.getMinTimeMs());
                eventMap.put("maxTimeMs", e.getMaxTimeMs());
                return eventMap;
            }).toList());
            report.put("events", eventsSection);

            // Write to file
            Path exportDir = plugin.getExportDirectory();
            Files.createDirectories(exportDir);

            String filename = "profiler-report-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".json";
            Path exportFile = exportDir.resolve(filename);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(exportFile.toFile())) {
                gson.toJson(report, writer);
            }

            sendMessage(playerData, "Report exported to: " + exportFile.getFileName(), GREEN);

        } catch (IOException e) {
            sendMessage(playerData, "Failed to export report: " + e.getMessage(), RED);
        }
    }

    private void triggerGC(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (!player.hasPermission("profiler.admin")) {
            sendMessage(playerData, "You need profiler.admin permission to trigger GC.", RED);
            return;
        }

        JVMData before = plugin.getJvmMetricsCollector().collect();
        plugin.getJvmMetricsCollector().triggerGC();

        // Give GC a moment to run
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        JVMData after = plugin.getJvmMetricsCollector().collect();

        long freed = before.getHeapUsed() - after.getHeapUsed();
        sendMessage(playerData, "Garbage collection triggered.", GREEN);
        sendMessage(playerData, String.format("Memory freed: %s", FormatUtil.formatBytes(Math.max(0, freed))), GRAY);
        sendMessage(playerData, String.format("Heap now: %s / %s (%s)",
            FormatUtil.formatBytes(after.getHeapUsed()),
            FormatUtil.formatBytes(after.getHeapMax()),
            FormatUtil.formatPercent(after.getHeapPercentage())), GRAY);
    }

    private void resetMetrics(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (!player.hasPermission("profiler.admin")) {
            sendMessage(playerData, "You need profiler.admin permission to reset metrics.", RED);
            return;
        }

        // Reset event timing statistics
        plugin.getEventTimingCollector().reset();
        sendMessage(playerData, "Event timing statistics have been reset.", GREEN);

        // Note: ECS system metrics are managed by Hytale's HistoricMetric and cannot be reset
        sendMessage(playerData, "Note: ECS system metrics are managed by Hytale and reset over time.", GRAY);
    }

    private void sendMessage(PlayerRef playerData, String text, Color color) {
        playerData.sendMessage(Message.raw(text).color(color));
    }
}
