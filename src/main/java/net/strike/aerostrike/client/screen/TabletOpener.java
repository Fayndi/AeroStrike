package net.strike.aerostrike.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.strike.aerostrike.client.screen.xaero.XaeroTacticalOpener;

/**
 * Dedicated client-side screen opener for the Military Tablet.
 * Intelligently routes between Xaero's World Map fullscreen tactical HUD
 * and the standalone radar map.
 */
@OnlyIn(Dist.CLIENT)
public final class TabletOpener {

    public static void open(ItemStack stack) {
        if (ModList.get().isLoaded("xaeroworldmap")) {
            XaeroTacticalOpener.open(stack);
        } else {
            Minecraft.getInstance().setScreen(new TacticalRadarScreen(stack));
        }
    }

    private TabletOpener() {}
}
