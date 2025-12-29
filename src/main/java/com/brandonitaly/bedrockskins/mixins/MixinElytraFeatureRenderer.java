package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if <1.21.11 {
/*import net.minecraft.client.render.VertexConsumerProvider;*/
//?} else {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?}

@Mixin(ElytraFeatureRenderer.class)
public abstract class MixinElytraFeatureRenderer {

    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$applyOffset(MatrixStack matrices, BipedEntityRenderState state) {
        // Use pattern matching or casting depending on Java version support
        if (state instanceof BedrockSkinState) {
            UUID uuid = ((BedrockSkinState) state).getUniqueId();
            if (uuid != null) {
                var model = BedrockModelManager.getModel(uuid);
                if (model != null && model.capeYOffset != 0f) {
                    matrices.push();
                    matrices.translate(0.0, model.capeYOffset * 0.0625f, 0.0);
                    pushed.set(true);
                }
            }
        }
    }

    @Unique
    private void bedrockSkins$resetMatrix(MatrixStack matrices) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }

    // --- Injectors ---

    //? if <1.21.11 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$applyOffset(matrices, state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }*/
    //?} else {
    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$applyOffset(matrices, state);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }
    //?}
}