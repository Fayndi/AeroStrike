package net.strike.aerostrike.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.strike.aerostrike.AeroStrike;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeroStrike.MOD_ID);

    public static final RegistryObject<CreativeModeTab> AEROSTRIKE_TAB = CREATIVE_MODE_TABS.register("aerostrike_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MILITARY_TABLET.get()))
                    .title(Component.translatable("itemGroup.aerostrike"))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(itemRegistryObject -> output.accept(itemRegistryObject.get()));
                    })
                    .build());

    private ModCreativeTabs() {}
}
