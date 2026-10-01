package net.strike.aerostrike.client.renderer.missile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.strike.aerostrike.client.model.Fp5FlamingoModel;
import net.strike.aerostrike.common.entity.missile.Fp5FlamingoEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Fp5FlamingoRenderer extends GeoEntityRenderer<Fp5FlamingoEntity> {

    public Fp5FlamingoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Fp5FlamingoModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    protected void applyRotations(Fp5FlamingoEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());

        // Align model heading to flight velocity vector (nose is +Z in model space)
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Align model attitude to vertical flight angle (negative pitch rotates nose up when climbing)
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}
