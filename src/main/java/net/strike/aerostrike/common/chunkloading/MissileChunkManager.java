package net.strike.aerostrike.common.chunkloading;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.UUID;

/**
 * Chunkloading manager for supersonic cruise and ballistic missiles.
 *
 * Prevents entities from hitting unloaded chunks and freezing in mid-air.
 * Automatically manages region tickets and ensures they are safely released
 * upon detonation, removal, or level unload to avoid memory leaks.
 */
public final class MissileChunkManager {
    // 2 chunks radius around the missile = 5x5 chunks loaded
    public static final int TICKET_RADIUS = 2;

    public static final TicketType<UUID> MISSILE_TICKET = TicketType.create(
            "aerostrike_missile",
            Comparator.comparing(UUID::toString)
    );

    /**
     * Updates or creates the region ticket for the missile's current chunk.
     */
    public static void forceChunk(ServerLevel level, Entity entity) {
        ChunkPos currentChunk = entity.chunkPosition();
        level.getChunkSource().addRegionTicket(MISSILE_TICKET, currentChunk, TICKET_RADIUS, entity.getUUID());
    }

    /**
     * Releases the region ticket.
     * MUST be called during Entity#remove(RemovalReason) / Entity#onRemovedFromWorld!
     */
    public static void releaseTicket(ServerLevel level, Entity entity) {
        ChunkPos currentChunk = entity.chunkPosition();
        level.getChunkSource().removeRegionTicket(MISSILE_TICKET, currentChunk, TICKET_RADIUS, entity.getUUID());
    }

    private MissileChunkManager() {}
}
