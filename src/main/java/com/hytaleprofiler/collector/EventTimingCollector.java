package com.hytaleprofiler.collector;

import com.hytaleprofiler.data.EventProfile;
import com.hytaleprofiler.util.FormatUtil;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.server.core.event.events.player.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects timing data for event handlers by registering
 * timing hooks at FIRST and LAST priority.
 */
public class EventTimingCollector {

    // Thread-local storage for event start times
    private final ThreadLocal<Map<Class<?>, Long>> eventStartTimes =
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    // Profiles indexed by event class
    private final Map<Class<?>, EventProfile> profiles = new ConcurrentHashMap<>();

    // Track if we've registered handlers
    private boolean registered = false;

    /**
     * Register timing hooks for common event types.
     */
    public void registerTimingHooks(EventRegistry eventRegistry) {
        if (registered) return;
        registered = true;

        // Register each event type individually to avoid generic type inference issues
        registerPlayerInteractTiming(eventRegistry);
        registerPlayerConnectTiming(eventRegistry);
        registerPlayerDisconnectTiming(eventRegistry);
        registerPlayerChatTiming(eventRegistry);
        registerPlayerReadyTiming(eventRegistry);
        registerAddPlayerToWorldTiming(eventRegistry);

        // Note: ECS events (BreakBlockEvent, PlaceBlockEvent, etc.) go through
        // EntityEventSystem and are already tracked in system metrics.
        // We focus on IBaseEvent events here.
    }

    private void registerPlayerInteractTiming(EventRegistry eventRegistry) {
        Class<PlayerInteractEvent> eventClass = PlayerInteractEvent.class;
        profiles.put(eventClass, new EventProfile("PlayerInteract", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    private void registerPlayerConnectTiming(EventRegistry eventRegistry) {
        Class<PlayerConnectEvent> eventClass = PlayerConnectEvent.class;
        profiles.put(eventClass, new EventProfile("PlayerConnect", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    private void registerPlayerDisconnectTiming(EventRegistry eventRegistry) {
        Class<PlayerDisconnectEvent> eventClass = PlayerDisconnectEvent.class;
        profiles.put(eventClass, new EventProfile("PlayerDisconnect", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    private void registerPlayerChatTiming(EventRegistry eventRegistry) {
        Class<PlayerChatEvent> eventClass = PlayerChatEvent.class;
        profiles.put(eventClass, new EventProfile("PlayerChat", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    private void registerPlayerReadyTiming(EventRegistry eventRegistry) {
        Class<PlayerReadyEvent> eventClass = PlayerReadyEvent.class;
        profiles.put(eventClass, new EventProfile("PlayerReady", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    private void registerAddPlayerToWorldTiming(EventRegistry eventRegistry) {
        Class<AddPlayerToWorldEvent> eventClass = AddPlayerToWorldEvent.class;
        profiles.put(eventClass, new EventProfile("AddPlayerToWorld", eventClass.getName()));

        eventRegistry.registerGlobal(EventPriority.FIRST, eventClass, event -> {
            eventStartTimes.get().put(eventClass, System.nanoTime());
        });
        eventRegistry.registerGlobal(EventPriority.LAST, eventClass, event -> {
            recordTiming(eventClass);
        });
    }

    /**
     * Record timing for an event class.
     */
    private void recordTiming(Class<?> eventClass) {
        Long startTime = eventStartTimes.get().remove(eventClass);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            EventProfile profile = profiles.get(eventClass);
            if (profile != null) {
                profile.record(duration);
            }
        }
    }

    /**
     * Get all event profiles, sorted by total time descending.
     */
    public List<EventProfile> getProfiles() {
        List<EventProfile> result = new ArrayList<>(profiles.values());
        Collections.sort(result);
        return result;
    }

    /**
     * Get profiles that have been called at least once.
     */
    public List<EventProfile> getActiveProfiles() {
        List<EventProfile> result = new ArrayList<>();
        for (EventProfile profile : profiles.values()) {
            if (profile.getCallCount() > 0) {
                result.add(profile);
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Get total time spent processing all events.
     */
    public double getTotalEventTimeMs() {
        double total = 0;
        for (EventProfile profile : profiles.values()) {
            total += profile.getTotalTimeMs();
        }
        return total;
    }

    /**
     * Get total event call count.
     */
    public long getTotalEventCount() {
        long total = 0;
        for (EventProfile profile : profiles.values()) {
            total += profile.getCallCount();
        }
        return total;
    }

    /**
     * Reset all event timing statistics.
     */
    public void reset() {
        for (EventProfile profile : profiles.values()) {
            profile.reset();
        }
    }

    /**
     * Check if timing hooks have been registered.
     */
    public boolean isRegistered() {
        return registered;
    }
}
