package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

@Mixin(WingsLayer.class)
public abstract class MixinElytraFeatureRenderer {

    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$applyOffset(PoseStack matrices, HumanoidRenderState state) {
        // Use pattern matching or casting depending on Java version support
        if (state instanceof BedrockSkinState) {
            UUID uuid = ((BedrockSkinState) state).getUniqueId();
            if (uuid != null) {
                var skinId = SkinManager.getSkin(uuid.toString());
                BedrockPlayerModel model = skinId == null ? null : BedrockModelManager.getModel(skinId);
                if (model != null && model.capeYOffset != 0f) {
                    matrices.pushPose();
                    matrices.translate(0.0, model.capeYOffset * 0.0625f, 0.0);
                    pushed.set(true);
                }
            }
        }
    }

    @Unique
    private void bedrockSkins$resetMatrix(PoseStack matrices) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.popPose(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }

    // --- Injectors ---

    //? if <=1.21.8 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$applyOffset(matrices, state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }*/
    //?} else {
    @Inject(method = "submit", at = @At("HEAD"))
    private void beforeRender(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$applyOffset(matrices, state);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void afterRender(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }
    //?}
}