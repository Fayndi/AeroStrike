package net.strike.aerostrike.client.tracker;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of all cruise missiles currently tracked by the Military Tablet.
 * Synchronized from the server in real-time across the entire Minecraft world map.
 */
public final class ClientMissileTracker {

    public record TrackedMissile(
            int entityId,
            String name,
            double x, double y, double z,
            float yaw, float pitch,
            String phaseName,
            float speed,
            BlockPos targetPos,
            long lastSeenTime
    ) {}

    private static final Map<Integer, TrackedMissile> TRACKED = new ConcurrentHashMap<>();

    public static void updateMissile(TrackedMissile missile) {
        TRACKED.put(missile.entityId(), missile);
    }

    public static void clearExpired() {
        long now = System.currentTimeMillis();
        TRACKED.entrySet().removeIf(entry -> now - entry.getValue().lastSeenTime() > 4000);
    }

    public static Collection<TrackedMissile> getActiveMissiles() {
        clearExpired();
        return TRACKED.values();
    }

    public static void remove(int entityId) {
        TRACKED.remove(entityId);
    }

    public static void clearAll() {
        TRACKED.clear();
    }

    private ClientMissileTracker() {}
}
