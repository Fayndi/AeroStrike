package net.strike.aerostrike.client.model;

import net.minecraft.resources.ResourceLocation;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.common.entity.missile.StormShadowEntity;
import software.bernie.geckolib.model.GeoModel;

public class StormShadowModel extends GeoModel<StormShadowEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(AeroStrike.MOD_ID, "geo/entity/storm_shadow.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(AeroStrike.MOD_ID, "textures/entity/storm_shadow.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(AeroStrike.MOD_ID, "animations/entity/storm_shadow.animation.json");

    @Override
    public ResourceLocation getModelResource(StormShadowEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StormShadowEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StormShadowEntity animatable) {
        return ANIMATION;
    }
}
