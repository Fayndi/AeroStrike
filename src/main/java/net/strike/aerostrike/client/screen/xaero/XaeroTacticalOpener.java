package net.strike.aerostrike.client.screen.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.strike.aerostrike.client.screen.TacticalRadarScreen;
import xaero.map.WorldMapSession;

/**
 * Isolated bridge to open TacticalXaeroMapScreen.
 * Safely separated so no Xaero classes are referenced until verified that the mod is loaded.
 */
@OnlyIn(Dist.CLIENT)
public final class XaeroTacticalOpener {

    public static void open(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        WorldMapSession session = WorldMapSession.getCurrentSession();

        if (session != null && session.getMapProcessor() != null && mc.player != null) {
            mc.setScreen(new TacticalXaeroMapScreen(
                    mc.screen,
                    null,
                    session.getMapProcessor(),
                    mc.player,
                    stack
            ));
        } else {
            // Fallback to standalone radar screen if Xaero session is not ready
            mc.setScreen(new TacticalRadarScreen(stack));
        }
    }

    private XaeroTacticalOpener() {}
}
