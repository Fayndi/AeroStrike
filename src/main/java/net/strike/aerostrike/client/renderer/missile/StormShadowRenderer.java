package net.strike.aerostrike.client.renderer.missile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.strike.aerostrike.client.model.StormShadowModel;
import net.strike.aerostrike.common.entity.missile.StormShadowEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StormShadowRenderer extends GeoEntityRenderer<StormShadowEntity> {

    public StormShadowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StormShadowModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(StormShadowEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Calculate smooth rotation interpolation
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}
