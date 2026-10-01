package net.strike.aerostrike.client.model;

import net.minecraft.resources.ResourceLocation;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.entity.missile.Fp5FlamingoEntity;
import software.bernie.geckolib.model.GeoModel;

public class Fp5FlamingoModel extends GeoModel<Fp5FlamingoEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(AeroStrike.MOD_ID, "geo/entity/fp5_flamingo.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(AeroStrike.MOD_ID, "textures/entity/fp5_flamingo.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(AeroStrike.MOD_ID, "animations/entity/fp5_flamingo.animation.json");

    @Override
    public ResourceLocation getModelResource(Fp5FlamingoEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Fp5FlamingoEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Fp5FlamingoEntity animatable) {
        return ANIMATION;
    }
}
