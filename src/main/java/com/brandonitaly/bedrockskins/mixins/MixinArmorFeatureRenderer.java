package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinArmorFeatureRenderer {

    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$configureVisibility(UUID uuid, boolean isPlayerRenderState, boolean stateCapeVisible) {
        if (uuid == null) return;
        var skinId = SkinManager.getSkin(uuid.toString());
        var model = skinId == null ? null : BedrockModelManager.getModel(skinId);
        if (!(model instanceof BedrockPlayerModel bedrockModel)) return;

        // 1. Default to visible
        bedrockModel.setBedrockPartVisible("bodyArmor", true);
        bedrockModel.setBedrockPartVisible("helmet", true);

        // 2. Check for Cape
        boolean hasCape = false;

        // Check Vanilla Flag & Actual Texture
        if (isPlayerRenderState && stateCapeVisible) {
            hasCape = bedrockSkins$hasCapeTexture(uuid);
        }

        // Fallback: Check Model Geometry
        if (!hasCape) {
            hasCape = bedrockModel.partsMap.containsKey("cape") || bedrockModel.partsMap.containsKey("elytra");
        }

        // 3. Hide Body Armor if Cape exists
        if (hasCape) {
            bedrockModel.setBedrockPartVisible("bodyArmor", false);
        }
    }

    @Unique
    private boolean bedrockSkins$hasCapeTexture(UUID uuid) {
        try {
            var client = Minecraft.getInstance();
            if (client == null || client.getConnection() == null) return false;
            var entry = client.getConnection().getPlayerInfo(uuid);
            if (entry == null) return false;
            var textures = entry.getSkin();
            if (textures == null) return false;

            //? if <=1.21.8 {
            /*
            // Reflection fallback for older versions
            java.lang.reflect.Method capeMethod = textures.getClass().getMethod("cape");
            return capeMethod.invoke(textures) != null;
            */
            //?} else {
            return textures.cape() != null;
            //?}
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private void bedrockSkins$applyArmorOffset(PoseStack matrices, Object modelObj, ItemStack stack, EquipmentSlot slot) {
        if (!(modelObj instanceof BedrockPlayerModel bedrockModel)) return;

        // Hiding Logic
        if (!stack.isEmpty()) {
            if (slot == EquipmentSlot.CHEST) bedrockModel.setBedrockPartVisible("bodyArmor", false);
            else if (slot == EquipmentSlot.HEAD) bedrockModel.setBedrockPartVisible("helmet", false);
        }

        // Offset Logic
        float pixels = 0f;
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST) {
            pixels = bedrockModel.upperArmorYOffset;
        }
        
        if (pixels != 0f) {
            matrices.pushPose();
            matrices.translate(0.0, pixels * 0.0625f, 0.0);
            pushed.set(true);
        }
    }

    @Unique
    private void bedrockSkins$resetMatrix(PoseStack matrices) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.popPose(); } catch (Exception ignored) {}
            pushed.remove();
        }
    }

    // --- Injectors ---

    //? if <=1.21.8 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState skinState) ? skinState.getUniqueId() : null;
        boolean isPlayerState = state instanceof PlayerEntityRenderState;
        boolean capeVisible = isPlayerState && ((PlayerEntityRenderState) state).capeVisible;
        bedrockSkins$configureVisibility(uuid, isPlayerState, capeVisible);
    }

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void beforeRenderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, EquipmentSlot slot, int light, BipedEntityModel<?> modelArg, CallbackInfo ci) {
        // Old version: derive model from argument
        bedrockSkins$applyArmorOffset(matrices, modelArg, stack, slot);
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void afterRenderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, EquipmentSlot slot, int light, BipedEntityModel<?> modelArg, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }*/
    //?} else {
    @Inject(method = "submit", at = @At("HEAD"))
    private void onRenderHead(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState skinState) ? skinState.getUniqueId() : null;
        boolean isPlayerState = state instanceof AvatarRenderState;
        boolean capeVisible = isPlayerState && ((AvatarRenderState) state).showCape;
        bedrockSkins$configureVisibility(uuid, isPlayerState, capeVisible);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void beforeRenderArmor(PoseStack matrices, SubmitNodeCollector queue, ItemStack stack, EquipmentSlot slot, int light, HumanoidRenderState state, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState skinState) ? skinState.getUniqueId() : null;
        var skinId = uuid == null ? null : SkinManager.getSkin(uuid.toString());
        var model = (skinId != null) ? BedrockModelManager.getModel(skinId) : null;
        bedrockSkins$applyArmorOffset(matrices, model, stack, slot);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void afterRenderArmor(PoseStack matrices, SubmitNodeCollector queue, ItemStack stack, EquipmentSlot slot, int light, HumanoidRenderState state, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }
    //?}
}