package net.strike.aerostrike.core.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.entity.missile.Fp5FlamingoEntity;
import net.strike.aerostrike.common.entity.missile.StormShadowEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AeroStrike.MOD_ID);

    public static final RegistryObject<EntityType<Fp5FlamingoEntity>> FP5_FLAMINGO =
            ENTITIES.register("fp5_flamingo", () -> EntityType.Builder.<Fp5FlamingoEntity>of(Fp5FlamingoEntity::new, MobCategory.MISC)
                    .sized(1.2f, 0.9f)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .fireImmune()
                    .build(new ResourceLocation(AeroStrike.MOD_ID, "fp5_flamingo").toString()));

    public static final RegistryObject<EntityType<StormShadowEntity>> STORM_SHADOW =
            ENTITIES.register("storm_shadow", () -> EntityType.Builder.<StormShadowEntity>of(StormShadowEntity::new, MobCategory.MISC)
                    .sized(1.1f, 0.8f)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .fireImmune()
                    .build(new ResourceLocation(AeroStrike.MOD_ID, "storm_shadow").toString()));

    private ModEntities() {}
}
