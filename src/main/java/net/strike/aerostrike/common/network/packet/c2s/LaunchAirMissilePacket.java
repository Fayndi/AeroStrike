package net.strike.aerostrike.common.network.packet.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.strike.aerostrike.common.entity.missile.StormShadowEntity;
import net.strike.aerostrike.core.registry.ModEntities;
import net.strike.aerostrike.core.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S packet sent from Military Tablet to launch an air-dropped cruise missile (e.g. Storm Shadow)
 * with designated target, intermediate flight waypoints, cruise altitude, and salvo count.
 */
public class LaunchAirMissilePacket {

    private final BlockPos targetPos;
    private final List<BlockPos> waypoints;
    private final float cruiseAltitude;
    private final int salvoCount;

    public LaunchAirMissilePacket(BlockPos targetPos, List<BlockPos> waypoints, float cruiseAltitude, int salvoCount) {
        this.targetPos = targetPos;
        this.waypoints = waypoints != null ? waypoints : new ArrayList<>();
        this.cruiseAltitude = cruiseAltitude;
        this.salvoCount = Math.max(1, Math.min(4, salvoCount));
    }

    public static void encode(LaunchAirMissilePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.targetPos);
        buf.writeVarInt(packet.waypoints.size());
        for (BlockPos wp : packet.waypoints) {
            buf.writeBlockPos(wp);
        }
        buf.writeFloat(packet.cruiseAltitude);
        buf.writeVarInt(packet.salvoCount);
    }

    public static LaunchAirMissilePacket decode(FriendlyByteBuf buf) {
        BlockPos target = buf.readBlockPos();
        int wpCount = buf.readVarInt();
        List<BlockPos> waypoints = new ArrayList<>(wpCount);
        for (int i = 0; i < wpCount; i++) {
            waypoints.add(buf.readBlockPos());
        }
        float alt = buf.readFloat();
        int salvo = buf.readVarInt();
        return new LaunchAirMissilePacket(target, waypoints, alt, salvo);
    }

    public static void handle(LaunchAirMissilePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            boolean isCreative = player.getAbilities().instabuild;
            int missilesToLaunch = packet.salvoCount;

            // Check inventory count for Storm Shadow
            int availableMissiles = 0;
            if (!isCreative) {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.is(ModItems.STORM_SHADOW_ITEM.get())) {
                        availableMissiles += stack.getCount();
                    }
                }
                missilesToLaunch = Math.min(missilesToLaunch, availableMissiles);
            }

            if (missilesToLaunch <= 0) {
                player.displayClientMessage(
                        Component.literal("§c[AeroStrike] Ошибка: В инвентаре нет ракет Storm Shadow для сброса!"),
                        true
                );
                return;
            }

            // Consume items from inventory if not creative
            if (!isCreative) {
                int toConsume = missilesToLaunch;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.is(ModItems.STORM_SHADOW_ITEM.get())) {
                        int take = Math.min(toConsume, stack.getCount());
                        stack.shrink(take);
                        toConsume -= take;
                        if (toConsume <= 0) break;
                    }
                }
            }

            // Launch the missiles with spacing
            Vec3 playerLook = player.getLookAngle();
            Vec3 rightVec = new Vec3(-playerLook.z, 0, playerLook.x).normalize();

            for (int i = 0; i < missilesToLaunch; i++) {
                StormShadowEntity missile = ModEntities.STORM_SHADOW.get().create(serverLevel);
                if (missile == null) continue;

                // Spread offset for salvo launch so missiles do not collide
                double lateralOffset = (i - (missilesToLaunch - 1) / 2.0) * 2.5;
                Vec3 spawnPos = player.position()
                        .add(0, -1.2, 0)
                        .add(rightVec.scale(lateralOffset))
                        .add(playerLook.scale(i * 1.5));

                missile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                missile.setCustomCruiseClearance(packet.cruiseAltitude);
                missile.setWaypoints(packet.waypoints);

                serverLevel.addFreshEntity(missile);
                missile.launch(packet.targetPos);
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 2.0f, 0.8f);

            player.displayClientMessage(
                    Component.literal("§a[AeroStrike] §fЗалп §e" + missilesToLaunch + "x Storm Shadow §fпроизведен! Цель: §e" +
                            packet.targetPos.getX() + ", " + packet.targetPos.getY() + ", " + packet.targetPos.getZ() +
                            (packet.waypoints.isEmpty() ? "" : " §f| ППМ: §6" + packet.waypoints.size())),
                    true
            );
        });
        ctx.setPacketHandled(true);
    }
}
