package com.hytaleprofiler;

import com.hytaleprofiler.collector.EntityCollector;
import com.hytaleprofiler.collector.JVMMetricsCollector;
import com.hytaleprofiler.collector.SystemMetricsCollector;
import com.hytaleprofiler.collector.TPSCollector;
import com.hytaleprofiler.command.ProfilerCommand;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.nio.file.Path;

/**
 * HytaleProfiler - Server profiler for Hytale servers.
 * Provides deep insight into TPS bottlenecks, system performance, entity counts, and memory usage.
 */
public class HytaleProfiler extends JavaPlugin {

    private static HytaleProfiler instance;
    private HytaleLogger logger;

    // Collectors
    private TPSCollector tpsCollector;
    private SystemMetricsCollector systemMetricsCollector;
    private EntityCollector entityCollector;
    private JVMMetricsCollector jvmMetricsCollector;

    public HytaleProfiler(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    public void setup() {
        logger = getLogger();
        logger.atInfo().log("Setting up HytaleProfiler...");

        // Initialize collectors
        tpsCollector = new TPSCollector();
        systemMetricsCollector = new SystemMetricsCollector();
        entityCollector = new EntityCollector();
        jvmMetricsCollector = new JVMMetricsCollector();

        // Register command
        getCommandRegistry().registerCommand(new ProfilerCommand(this));

        logger.atInfo().log("HytaleProfiler setup complete.");
    }

    @Override
    public void start() {
        logger.atInfo().log("HytaleProfiler started.");
    }

    @Override
    public void shutdown() {
        logger.atInfo().log("HytaleProfiler shutting down.");
    }

    public static HytaleProfiler getInstance() {
        return instance;
    }

    public TPSCollector getTpsCollector() {
        return tpsCollector;
    }

    public SystemMetricsCollector getSystemMetricsCollector() {
        return systemMetricsCollector;
    }

    public EntityCollector getEntityCollector() {
        return entityCollector;
    }

    public JVMMetricsCollector getJvmMetricsCollector() {
        return jvmMetricsCollector;
    }

    public Path getExportDirectory() {
        return getDataDirectory();
    }
}
