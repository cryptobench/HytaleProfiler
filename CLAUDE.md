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

## CRITICAL: Multi-Threaded Architecture & Thread Safety

**Hytale uses a multi-threaded server model. Understanding this is MANDATORY before writing any plugin code.**

### Core Architecture

| Component | Description |
|-----------|-------------|
| **HytaleServer** | Singleton root; owns `SCHEDULED_EXECUTOR` for background tasks |
| **Universe** | Singleton container for all worlds; thread-safe player lookups via `ConcurrentHashMap` |
| **World** | Each world runs on its **own dedicated thread** |

**Key Benefit:** Lag in "World A" does NOT cause lag in "World B" - worlds run in parallel.

### The Thread-Bound Rule (CRITICAL)

**The `EntityStore` and ALL ECS operations (`getComponent`, `addComponent`, `removeComponent`) are THREAD-BOUND.**

They can ONLY be accessed from their specific world's thread. Hytale uses `assertThread()` internally - accessing from the wrong thread throws `IllegalStateException` immediately to prevent silent data corruption.

```java
// WRONG - will crash if called from wrong thread
store.getComponent(playerRef, Player.getComponentType());

// CORRECT - ensures execution on world thread
world.execute(() -> {
    store.getComponent(playerRef, Player.getComponentType());
});
```

### The Bridge: `world.execute()`

To run code on a specific world's thread from an external thread (background task, different world, etc.), use `world.execute()`:

```java
// From a background task or different thread
world.execute(() -> {
    // This code runs safely on the world's thread
    Store<EntityStore> store = world.getEntityStore().getStore();
    // Now safe to access ECS components
});
```

### Thread-Safe vs Thread-Bound Operations

| Always Safe (Any Thread) | Unsafe (Requires `world.execute()`) |
|-------------------------|-------------------------------------|
| `Universe.get().getPlayer(uuid)` | `store.getComponent(ref, type)` |
| `playerRef.sendMessage(message)` | `store.addComponent(...)` |
| `HytaleServer.SCHEDULED_EXECUTOR.schedule(...)` | `store.removeComponent(...)` |
| `world.execute(runnable)` | Modifying entity position/health/inventory |

### Managing Shared Plugin State

When sharing data across multiple worlds (global state), use Java's thread-safe types:

```java
// Counters - use AtomicInteger
private final AtomicInteger globalKills = new AtomicInteger(0);
globalKills.incrementAndGet();

// Collections/Maps - use ConcurrentHashMap
private final ConcurrentHashMap<UUID, Integer> playerKills = new ConcurrentHashMap<>();
playerKills.merge(playerId, 1, Integer::sum);

// One-time initialization - use AtomicBoolean
private final AtomicBoolean initialized = new AtomicBoolean(false);
if (initialized.compareAndSet(false, true)) {
    // Initialize only once
}

// Simple flags - use volatile
private volatile boolean enabled = true;
```

### Common Mistakes & Patterns

#### The Executor Trap
`SCHEDULED_EXECUTOR` runs on its own background thread, NOT a world thread:
```java
// WRONG - crashes when touching entity
HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
    store.getComponent(ref, type);  // IllegalStateException!
}, 1, TimeUnit.SECONDS);

// CORRECT - bridge back to world thread
HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
    world.execute(() -> {
        store.getComponent(ref, type);  // Safe!
    });
}, 1, TimeUnit.SECONDS);
```

#### Avoid Blocking World Threads
Never call `.join()` or `.get()` on a `CompletableFuture` inside a world thread - it blocks the entire world tick:
```java
// WRONG - blocks world tick
CompletableFuture<Data> future = fetchDataAsync();
Data data = future.get();  // DON'T DO THIS

// CORRECT - use callbacks
fetchDataAsync().thenAccept(data -> {
    world.execute(() -> {
        // Process data on world thread
    });
});
```

#### Race Conditions
Remember that `counter++` is secretly three operations (read, increment, write):
```java
// WRONG - race condition
private int counter = 0;
counter++;  // Lost updates!

// CORRECT - atomic operation
private final AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
```

### Technical Specifications

| Spec | Value | Notes |
|------|-------|-------|
| **Tick Rate** | 30 TPS | 33.3ms per tick (vs Minecraft's 20 TPS) |
| **Tick Budget** | 33ms | Heavy logic (>33ms) lags the entire world |
| **Scaling** | Per-core | More CPU cores = more parallel worlds |

### Performance Best Practices

1. **Offload Heavy Work:** Move expensive operations (pathfinding, database I/O, HTTP requests) to `SCHEDULED_EXECUTOR` or `CompletableFuture.runAsync()`
2. **Avoid Object Creation in Ticks:** Reuse objects where possible to reduce GC pressure
3. **Use `world.execute()` Sparingly:** Queue minimal work back to world threads

### Local vs Global Events

| Event Type | Thread Context | Example |
|------------|---------------|---------|
| **Local Events** | Fires on the World Thread | `PlayerInteractEvent`, `BreakBlockEvent` - safe to touch ECS directly |
| **Global Events** | May fire on different thread | Server-wide events - must use `world.execute()` before touching entities |

### The Golden Rule

> **"Always assume you are on the wrong thread unless you are inside a standard World System or event handler. If you touch `store`, verify you are thread-bound or wrapped in `world.execute()`."**

### Debugging Thread Issues

If you see:
- `IllegalStateException: Assert not in thread!` → You're accessing ECS from wrong thread
- `IllegalStateException: Store is currently processing!` → You're modifying during iteration
- Random crashes or data corruption → Race condition, use atomic types

**First debug step:** "Is this code touching a Store/Component while running on an Executor thread?"

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
