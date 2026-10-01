package net.strike.aerostrike;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.strike.aerostrike.client.ClientSetup;
import net.strike.aerostrike.core.registry.*;
import org.slf4j.Logger;

/**
 * AeroStrike - Scalable military-technical Minecraft 1.20.1 Forge mod.
 *
 * Engineered with:
 * - Strict client/server separation for dedicated server stability.
 * - Decoupled data facades ready for immediate NeoForge 1.21.1 migration.
 * - Chunk ticket lifecycle management for supersonic missiles.
 */
@Mod(AeroStrike.MOD_ID)
public class AeroStrike {
    public static final String MOD_ID = "aerostrike";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroStrike(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 1. Register DeferredRegisters to Mod Event Bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // 2. Mod Lifecycle Listeners
        modEventBus.addListener(this::commonSetup);

        // 3. Client-safe initialization (Prevents ClassNotFoundException on Dedicated Server)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modEventBus));

        // 4. Register to Forge game event bus
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[AeroStrike] Core registries registered successfully.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            net.strike.aerostrike.common.network.ModNetwork.registerPackets();
            LOGGER.info("[AeroStrike] Common setup completed. ModNetwork registered.");
        });
    }
}
