package net.strike.aerostrike.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TabletOpener {
    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new TabletScreen(stack));
    }

    private TabletOpener() {}
}
