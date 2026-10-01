package net.strike.aerostrike.core.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.item.missile.CruiseMissileItem;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AeroStrike.MOD_ID);

    // Core electronics & military equipment
    public static final RegistryObject<Item> MILITARY_TABLET = ITEMS.register("military_tablet",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LASER_DESIGNATOR = ITEMS.register("laser_designator",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Deployable weapons / missiles
    public static final RegistryObject<Item> FP5_FLAMINGO_ITEM = ITEMS.register("fp5_flamingo",
            () -> new CruiseMissileItem<>(ModEntities.FP5_FLAMINGO, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CRUISE_MISSILE_ITEM = ITEMS.register("cruise_missile",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BALLISTIC_MISSILE_ITEM = ITEMS.register("ballistic_missile",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FPV_DRONE_ITEM = ITEMS.register("fpv_drone",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
