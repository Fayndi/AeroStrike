package net.strike.aerostrike.common.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.entity.base.AbstractCruiseMissileEntity;
import net.strike.aerostrike.common.network.packet.s2c.SyncMissilesPacket;
import net.strike.aerostrike.core.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side broadcaster that periodically transmits active cruise missile positions
 * to operators viewing or holding military tablets.
 */
@Mod.EventBusSubscriber(modid = AeroStrike.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MissileTrackerServer {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        // Broadcast every 4 ticks (~5 times per second)
        if (tickCounter % 4 != 0) return;

        if (event.getServer() == null) return;

        // Check if any players have tablet equipped or in inventory
        List<ServerPlayer> interestedPlayers = new ArrayList<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.getMainHandItem().is(ModItems.MILITARY_TABLET.get()) ||
                player.getOffhandItem().is(ModItems.MILITARY_TABLET.get())) {
                interestedPlayers.add(player);
            }
        }

        if (interestedPlayers.isEmpty()) return;

        // Collect all active cruise missiles across all loaded levels
        List<SyncMissilesPacket.MissileData> missileList = new ArrayList<>();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (AbstractCruiseMissileEntity missile : level.getEntitiesOfClass(AbstractCruiseMissileEntity.class,
                    new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 384, 30000000),
                    m -> m.isAlive() && m.getFlightPhase() != AbstractCruiseMissileEntity.FlightPhase.STANDBY)) {

                String phaseName = missile.getFlightPhase().name();
                float speed = (float) (missile.getDeltaMovement().length() * 20.0); // Blocks per second
                missileList.add(new SyncMissilesPacket.MissileData(
                        missile.getId(),
                        missile.getType().getDescription().getString(),
                        missile.getX(), missile.getY(), missile.getZ(),
                        missile.getYRot(), missile.getXRot(),
                        phaseName,
                        speed,
                        missile.getTargetPos()
                ));
            }
        }

        // Send telemetry packet to all interested players
        SyncMissilesPacket packet = new SyncMissilesPacket(missileList);
        for (ServerPlayer player : interestedPlayers) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private MissileTrackerServer() {}
}
