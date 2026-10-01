package net.strike.aerostrike.common.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.strike.aerostrike.client.tracker.ClientMissileTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C packet broadcasting active cruise missile telemetry to players holding military tablets.
 */
public class SyncMissilesPacket {

    public record MissileData(
            int entityId,
            String name,
            double x, double y, double z,
            float yaw, float pitch,
            String phaseName,
            float speed,
            BlockPos targetPos
    ) {}

    private final List<MissileData> missiles;

    public SyncMissilesPacket(List<MissileData> missiles) {
        this.missiles = missiles != null ? missiles : new ArrayList<>();
    }

    public static void encode(SyncMissilesPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.missiles.size());
        for (MissileData m : packet.missiles) {
            buf.writeVarInt(m.entityId());
            buf.writeUtf(m.name());
            buf.writeDouble(m.x());
            buf.writeDouble(m.y());
            buf.writeDouble(m.z());
            buf.writeFloat(m.yaw());
            buf.writeFloat(m.pitch());
            buf.writeUtf(m.phaseName());
            buf.writeFloat(m.speed());
            buf.writeBoolean(m.targetPos() != null);
            if (m.targetPos() != null) {
                buf.writeBlockPos(m.targetPos());
            }
        }
    }

    public static SyncMissilesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<MissileData> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int id = buf.readVarInt();
            String name = buf.readUtf();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            String phase = buf.readUtf();
            float speed = buf.readFloat();
            BlockPos target = buf.readBoolean() ? buf.readBlockPos() : null;
            list.add(new MissileData(id, name, x, y, z, yaw, pitch, phase, speed, target));
        }
        return new SyncMissilesPacket(list);
    }

    public static void handle(SyncMissilesPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                long now = System.currentTimeMillis();
                for (MissileData data : packet.missiles) {
                    ClientMissileTracker.updateMissile(new ClientMissileTracker.TrackedMissile(
                            data.entityId(),
                            data.name(),
                            data.x(), data.y(), data.z(),
                            data.yaw(), data.pitch(),
                            data.phaseName(),
                            data.speed(),
                            data.targetPos(),
                            now
                    ));
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
