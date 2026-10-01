package net.strike.aerostrike.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.client.renderer.missile.Fp5FlamingoRenderer;
import net.strike.aerostrike.core.registry.ModEntities;

/**
 * Dedicated Client initializer.
 * NEVER call, import, or reference this class from common or server code!
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::onClientSetup);
        modEventBus.addListener(ClientSetup::registerEntityRenderers);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AeroStrike.LOGGER.info("[AeroStrike] Client setup initialized: registering GeckoLib renderers, camera manager, and HUD overlays.");
        });
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FP5_FLAMINGO.get(), Fp5FlamingoRenderer::new);
    }

    private ClientSetup() {}
}
