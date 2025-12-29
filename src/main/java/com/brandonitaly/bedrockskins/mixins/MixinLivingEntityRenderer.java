package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21.11 {
/*import net.minecraft.client.render.VertexConsumerProvider;*/
//?} else {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
//?}

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @Shadow
    public EntityModel model;

    @Unique
    private Object originalModel;

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$swapModel(LivingEntityRenderState state) {
        if (state instanceof PlayerEntityRenderState && state instanceof BedrockSkinState skinState) {
            java.util.UUID uuid = skinState.getUniqueId();
            if (uuid != null) {
                var bedrockModel = BedrockModelManager.getModel(uuid);
                if (bedrockModel != null) {
                    originalModel = this.model;
                    this.model = (EntityModel) bedrockModel;
                    
                    if (originalModel instanceof PlayerEntityModel playerModel) {
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

    //? if <1.21.11 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        bedrockSkins$swapModel(state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        bedrockSkins$restoreModel();
    }*/
    //?} else {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        bedrockSkins$swapModel(state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        bedrockSkins$restoreModel();
    }
    //?}
}