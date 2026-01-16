# HytaleProfiler Plugin - Development Context

## Project Overview
A server profiler plugin for Hytale servers providing deep insight into TPS bottlenecks, system performance, entity counts, and memory usage.

## How to Explore the Hytale Server API

The Hytale server API is in `Server/HytaleServer.jar`. Since there's no official documentation, use these commands to discover available classes and methods:

### List All Classes in a Package
```bash
cd "/Users/golemgrid/Library/Application Support/Hytale/install/release/package/game/latest/Server"

# Find all metric-related classes
jar -tf HytaleServer.jar | grep -i "metric"

# Find all store-related classes
jar -tf HytaleServer.jar | grep -i "store"

# Find all classes in a specific package
jar -tf HytaleServer.jar | grep "com/hypixel/hytale/metrics/"
```

### Inspect a Class (Methods, Fields, Signatures)
```bash
# Basic class inspection
javap -classpath HytaleServer.jar com.hypixel.hytale.metrics.HistoricMetric

# With more detail (private members too)
javap -p -classpath HytaleServer.jar com.hypixel.hytale.component.Store
```

### Key Packages to Explore
```
com.hypixel.hytale.server.core.plugin        # JavaPlugin, PluginBase
com.hypixel.hytale.server.core.command       # Command system
com.hypixel.hytale.server.core.entity        # Entity/Player classes
com.hypixel.hytale.component                 # ECS system (Store, Ref, Query)
com.hypixel.hytale.metrics                   # HistoricMetric, JVMMetrics
com.hypixel.hytale.server.core.universe.world # World, TickingThread
```

## Hytale Metrics API

### Key APIs Used
| API | What it provides |
|-----|-----------------|
| `World.getBufferedTickLengthMetricSet()` | Tick timing history (World extends TickingThread) |
| `Store.getSystemMetrics()` | `HistoricMetric[]` for all ECS systems |
| `Store.getSystemNames()` | Names of registered systems |
| `Store.collectArchetypeChunkData()` | Entity breakdown by archetype |
| `HistoricMetric` | Time-series data with avg/min/max/history |

### HistoricMetric Methods
```java
// Get average value at history index (0 = most recent)
double avg = metric.getAverage(0);

// Get min/max values
double min = metric.getMin(0);
double max = metric.getMax(0);

// Get history size
int size = metric.getHistorySize();
```

## Plugin Structure
```
HytaleProfiler/
├── src/main/java/com/hytaleprofiler/
│   ├── HytaleProfiler.java           # Main plugin
│   ├── command/
│   │   └── ProfilerCommand.java      # All subcommands
│   ├── collector/
│   │   ├── TPSCollector.java         # TPS from TickingThread
│   │   ├── SystemMetricsCollector.java # ECS system timing
│   │   ├── EntityCollector.java      # Entity counts
│   │   └── JVMMetricsCollector.java  # Memory/GC stats
│   ├── data/
│   │   ├── TPSData.java
│   │   ├── SystemProfile.java
│   │   ├── ModProfile.java
│   │   ├── EntityData.java
│   │   └── JVMData.java
│   └── util/
│       └── FormatUtil.java           # Time/byte formatting
├── src/main/resources/
│   └── manifest.json
└── pom.xml
```

## Commands
| Command | Description |
|---------|-------------|
| `/profiler` | Show help |
| `/profiler summary` | Dashboard overview (TPS, memory, top slow mods) |
| `/profiler tps` | Detailed TPS and tick timing breakdown |
| `/profiler mods` | Per-mod timing breakdown (aggregated from systems) |
| `/profiler systems [count]` | ECS system timing breakdown, sorted by slowest |
| `/profiler top [count]` | Top N slowest systems |
| `/profiler entities` | Entity counts by type/archetype |
| `/profiler memory` | JVM heap, GC stats, thread count |
| `/profiler export` | Export full report to JSON file |
| `/profiler gc` | Trigger garbage collection (admin) |
| `/profiler reset` | Clear metrics history (admin) |

## Permissions
| Permission | Description |
|------------|-------------|
| `profiler.use` | View profiler data |
| `profiler.admin` | Admin commands (gc, reset, export) |

## Messages (No Minecraft Color Codes!)
```java
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

// Correct
playerData.sendMessage(Message.raw("Success!").color(new Color(85, 255, 85)));

// Common colors
Color GREEN = new Color(85, 255, 85);
Color RED = new Color(255, 85, 85);
Color YELLOW = new Color(255, 255, 85);
Color GOLD = new Color(255, 170, 0);
Color GRAY = new Color(170, 170, 170);
```

## Building & Installation
```bash
mvn clean package
# Copy target/HytaleProfiler-1.0.0.jar to Server/mods/
```

## CI/CD & Release Notes

Releases are automated via GitHub Actions. **Commit messages become release notes automatically.**

### Commit Message Guidelines
Write commit messages that make sense in a changelog:
```
Add per-mod performance breakdown command
Fix TPS calculation when server is heavily loaded
Update entity collector to show more detailed archetypes
```

When pushing to `master`, CI will:
1. Build the plugin
2. Create a GitHub release with auto-generated notes from commits
3. Attach the JAR file

## Key Imports
```java
// Plugin
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

// Metrics
import com.hypixel.hytale.metrics.HistoricMetric;

// ECS
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Commands
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

// Entity/Player
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

// Messages
import com.hypixel.hytale.server.core.Message;
```
