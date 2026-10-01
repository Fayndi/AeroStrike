package net.strike.aerostrike.core.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.strike.aerostrike.AeroStrike;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AeroStrike.MOD_ID);

    // Block entity types (e.g. RadarBlockEntity, SamLauncherBlockEntity) will be registered here

    private ModBlockEntities() {}
}
