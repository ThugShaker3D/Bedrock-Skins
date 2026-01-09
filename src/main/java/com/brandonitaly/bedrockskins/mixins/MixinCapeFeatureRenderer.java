package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;*/
//?}

@Mixin(CapeLayer.class)
public abstract class MixinCapeFeatureRenderer {
    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // Allow capes to use the translucent render layer instead of solid
    //? if >=1.21.11 {
    @Redirect(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
        )
    )
    private RenderType useTranslucentLayer(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
    //?} else {
    /*@Redirect(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private RenderType useTranslucentLayer(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }*/
    //?}

    // Bedrock Cape Positioning
    //? if <=1.21.8 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid == null) return;

        var model = BedrockModelManager.getModel(uuid);
        if (model != null && model.capeYOffset != 0f) {
            matrices.push();
            // 0.0625f converts pixels to block units
            double translateY = model.capeYOffset * 0.0625f;
            matrices.translate(0.0, translateY, 0.0);
            pushed.set(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }*/
    //?} else {
    @Inject(method = "submit", at = @At("HEAD"))
    private void beforeRender(PoseStack matrices, SubmitNodeCollector queue, int light, AvatarRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid == null) return;

        var model = BedrockModelManager.getModel(uuid);
        if (model != null && model.capeYOffset != 0f) {
            matrices.pushPose();
            // 0.0625f converts pixels to block units
            double translateY = model.capeYOffset * 0.0625f;
            matrices.translate(0.0, translateY, 0.0);
            pushed.set(true);
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void afterRender(PoseStack matrices, SubmitNodeCollector queue, int light, AvatarRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.popPose(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }
    //?}
}