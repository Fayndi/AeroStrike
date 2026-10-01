package net.strike.aerostrike.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.strike.aerostrike.AeroStrike;

/**
 * Dedicated Client initializer.
 * NEVER call, import, or reference this class from common or server code!
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::onClientSetup);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AeroStrike.LOGGER.info("[AeroStrike] Client setup initialized: registering renderers, camera manager, and HUD overlays.");
            // GeckoLib entity renderers, block entity renderers, and keybindings will be registered here
        });
    }

    private ClientSetup() {}
}
