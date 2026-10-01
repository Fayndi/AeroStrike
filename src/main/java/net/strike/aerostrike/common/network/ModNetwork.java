package net.strike.aerostrike.common.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.network.packet.c2s.LaunchAirMissilePacket;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AeroStrike.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void registerPackets() {
        CHANNEL.messageBuilder(LaunchAirMissilePacket.class, packetId++)
                .encoder(LaunchAirMissilePacket::encode)
                .decoder(LaunchAirMissilePacket::decode)
                .consumerMainThread(LaunchAirMissilePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    private ModNetwork() {}
}
