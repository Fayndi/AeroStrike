package net.strike.aerostrike.core.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.strike.aerostrike.AeroStrike;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AeroStrike.MOD_ID);

    // Entity types (missiles, bombs, drones, interceptors) will be registered here

    private ModEntities() {}
}
