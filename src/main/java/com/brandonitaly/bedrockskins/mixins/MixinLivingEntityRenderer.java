package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @Shadow
    public EntityModel model;

    @Unique
    private Object originalModel;

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$swapModel(LivingEntityRenderState state) {
        if (state instanceof AvatarRenderState && state instanceof BedrockSkinState skinState) {
            java.util.UUID uuid = skinState.getUniqueId();
            if (uuid != null) {
                var bedrockModel = BedrockModelManager.getModel(uuid);
                if (bedrockModel != null) {
                    originalModel = this.model;
                    this.model = (EntityModel) bedrockModel;
                    
                    if (originalModel instanceof PlayerModel playerModel) {
                        bedrockModel.copyFromVanilla(playerModel);
                    }
                }
            }
        }
    }

    @Unique
    private void bedrockSkins$restoreModel() {
        if (originalModel != null) {
            this.model = (EntityModel) originalModel;
            originalModel = null;
        }
    }

    // --- Injectors ---

    //? if <=1.21.8 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        bedrockSkins$swapModel(state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        bedrockSkins$restoreModel();
    }*/
    //?} else {
    @Inject(method = "submit", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        bedrockSkins$swapModel(state);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        bedrockSkins$restoreModel();
    }
    //?}
}