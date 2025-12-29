package com.brandonitaly.bedrockskins.mixins;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

//? if <1.21.11 {
/*import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;*/
//?} else {
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//?}

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    // --- Shared Helper Methods ---

    @Unique
    private void bedrockSkins$renderArm(boolean isRightArm, MatrixStack matrices, int light, Identifier skinTexture, boolean sleeveVisible, Object rendererOrQueue, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        var uuid = player.getUuid();
        var bedrockModel = BedrockModelManager.getModel(uuid);

        if (bedrockModel != null) {
            String side = isRightArm ? "right" : "left";
            String Side = isRightArm ? "Right" : "Left";

            // Get Arm Part
            String partKey = side + "_arm";
            var part = bedrockModel.partsMap.get(partKey);
            if (part == null) part = bedrockModel.partsMap.get(side + "Arm");

            // Get Sleeve Part
            String sleeveKey = side + "_sleeve";
            var sleeve = bedrockModel.partsMap.get(sleeveKey);
            if (sleeve == null) sleeve = bedrockModel.partsMap.get(side + "Sleeve");

            if (part != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                var bedrockSkin = (skinKey != null) ? SkinPackLoader.loadedSkins.get(skinKey) : null;
                Identifier texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

                final var finalPart = part;
                final var finalSleeve = sleeve;
                boolean sleeveIsChild = finalPart.hasChild(sleeveKey) || finalPart.hasChild(side + "Sleeve");

                //? if <1.21.11 {
                /*
                VertexConsumerProvider vertexConsumers = (VertexConsumerProvider) rendererOrQueue;
                var layer = RenderLayer.getEntityTranslucent(texture);
                
                MatrixStack ms = new MatrixStack();
                ms.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
                ms.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());

                finalPart.resetTransform();
                if (finalSleeve != null) {
                    finalSleeve.resetTransform();
                    finalSleeve.visible = sleeveVisible;
                }

                finalPart.render(ms, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                
                if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                    finalSleeve.render(ms, vertexConsumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                }
                */
                //?} else {
                OrderedRenderCommandQueue queue = (OrderedRenderCommandQueue) rendererOrQueue;
                var layer = RenderLayers.entityTranslucent(texture);

                queue.submitCustom(matrices, layer, (entry, consumer) -> {
                    MatrixStack ms = new MatrixStack();
                    ms.peek().getPositionMatrix().set(entry.getPositionMatrix());
                    ms.peek().getNormalMatrix().set(entry.getNormalMatrix());

                    finalPart.resetTransform();
                    if (finalSleeve != null) {
                        finalSleeve.resetTransform();
                        finalSleeve.visible = sleeveVisible;
                    }

                    finalPart.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);

                    if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                        finalSleeve.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);
                    }
                });
                //?}
                ci.cancel();
            }
        }
    }

    // --- Injectors ---

    //? if <1.21.11 {
    /*@Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof BedrockSkinState skinState) {
            skinState.setUniqueId(player.getUuid());
        }
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void renderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        bedrockSkins$renderArm(true, matrices, light, skinTexture, sleeveVisible, vertexConsumers, ci);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void renderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        bedrockSkins$renderArm(false, matrices, light, skinTexture, sleeveVisible, vertexConsumers, ci);
    }*/
    //?} else {
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayerEntity clientPlayer && state instanceof BedrockSkinState skinState) {
            skinState.setUniqueId(clientPlayer.getUuid());
        }
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void renderRightArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        bedrockSkins$renderArm(true, matrices, light, skinTexture, sleeveVisible, queue, ci);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void renderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        bedrockSkins$renderArm(false, matrices, light, skinTexture, sleeveVisible, queue, ci);
    }
    //?}

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void getTexture(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> ci) {
        if (state instanceof BedrockSkinState skinState) {
            java.util.UUID uuid = skinState.getUniqueId();
            if (uuid != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                if (skinKey != null) {
                    var skin = SkinPackLoader.loadedSkins.get(skinKey);
                    if (skin != null && skin.identifier != null) {
                        ci.setReturnValue(skin.identifier);
                    }
                }
            }
        }
    }
}