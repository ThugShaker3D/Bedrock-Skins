package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <=1.21.8 {
/*import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;*/
//?} else {
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
    //? if <1.21.11 {
    /*import net.minecraft.client.render.RenderLayer;*/
    //?} else {
    import net.minecraft.client.render.RenderLayers;
    //?}
//?}

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @Unique
    private void bedrockSkins$renderArm(boolean isRightArm, MatrixStack matrices, int light, Identifier skinTexture, boolean sleeveVisible, Object rendererOrQueue, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        var uuid = player.getUuid();
        var bedrockModel = BedrockModelManager.getModel(uuid);

        if (bedrockModel != null) {
            String side = isRightArm ? "right" : "left";
            
            var parts = bedrockModel.partsMap;
            var part = parts.get(side + "_arm");
            if (part == null) part = parts.get(side + "Arm");
            
            var sleeve = parts.get(side + "_sleeve");
            if (sleeve == null) sleeve = parts.get(side + "Sleeve");

            if (part != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                var bedrockSkin = (skinKey != null) ? SkinPackLoader.loadedSkins.get(skinKey) : null;
                Identifier texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

                final var finalPart = part;
                final var finalSleeve = sleeve;
                boolean sleeveIsChild = finalPart.hasChild(side + "_sleeve") || finalPart.hasChild(side + "Sleeve");

                //? if <=1.21.8 {
                /*
                // --- 1.21.8 Logic (VertexConsumerProvider) ---
                var consumers = (VertexConsumerProvider) rendererOrQueue;
                var layer = RenderLayer.getEntityTranslucent(texture);
                
                MatrixStack ms = new MatrixStack();
                ms.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
                ms.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());

                finalPart.resetTransform();
                if (finalSleeve != null) {
                    finalSleeve.resetTransform();
                    finalSleeve.visible = sleeveVisible;
                }

                finalPart.render(ms, consumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                    finalSleeve.render(ms, consumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                }
                */
                //?} else {
                // --- 1.21.9 & 1.21.11 Logic (OrderedRenderCommandQueue) ---
                var queue = (OrderedRenderCommandQueue) rendererOrQueue;
                
                //? if <1.21.11 {
                /*var layer = RenderLayer.getEntityTranslucent(texture);*/
                //?} else {
                var layer = RenderLayers.entityTranslucent(texture);
                //?}

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

    //? if <=1.21.8 {
    /*@Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof BedrockSkinState skinState) skinState.setUniqueId(player.getUuid());
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void renderRightArm(MatrixStack matrices, VertexConsumerProvider consumers, int light, Identifier tex, boolean sleeve, CallbackInfo ci) {
        bedrockSkins$renderArm(true, matrices, light, tex, sleeve, consumers, ci);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void renderLeftArm(MatrixStack matrices, VertexConsumerProvider consumers, int light, Identifier tex, boolean sleeve, CallbackInfo ci) {
        bedrockSkins$renderArm(false, matrices, light, tex, sleeve, consumers, ci);
    }*/
    //?} else {
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayerEntity cp && state instanceof BedrockSkinState skinState) {
            skinState.setUniqueId(cp.getUuid());
        }
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void renderRightArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier tex, boolean sleeve, CallbackInfo ci) {
        bedrockSkins$renderArm(true, matrices, light, tex, sleeve, queue, ci);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void renderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier tex, boolean sleeve, CallbackInfo ci) {
        bedrockSkins$renderArm(false, matrices, light, tex, sleeve, queue, ci);
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
                    if (skin != null && skin.identifier != null) ci.setReturnValue(skin.identifier);
                }
            }
        }
    }
}