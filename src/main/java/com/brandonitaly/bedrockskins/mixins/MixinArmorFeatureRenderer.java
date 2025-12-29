package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

//? if <=1.21.8 {
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
//?} else {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?}

@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinArmorFeatureRenderer {

    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$configureVisibility(UUID uuid, boolean isPlayerRenderState, boolean stateCapeVisible) {
        if (uuid == null) return;
        var model = BedrockModelManager.getModel(uuid);
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
            var client = MinecraftClient.getInstance();
            if (client == null || client.getNetworkHandler() == null) return false;
            var entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry == null) return false;
            var textures = entry.getSkinTextures();
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
    private void bedrockSkins$applyArmorOffset(MatrixStack matrices, Object modelObj, ItemStack stack, EquipmentSlot slot) {
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
            matrices.push();
            matrices.translate(0.0, pixels * 0.0625f, 0.0);
            pushed.set(true);
        }
    }

    @Unique
    private void bedrockSkins$resetMatrix(MatrixStack matrices) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) {}
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
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState skinState) ? skinState.getUniqueId() : null;
        boolean isPlayerState = state instanceof PlayerEntityRenderState;
        boolean capeVisible = isPlayerState && ((PlayerEntityRenderState) state).capeVisible;
        bedrockSkins$configureVisibility(uuid, isPlayerState, capeVisible);
    }

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void beforeRenderArmor(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
        // New version: derive model from state UUID
        UUID uuid = (state instanceof BedrockSkinState skinState) ? skinState.getUniqueId() : null;
        var model = (uuid != null) ? BedrockModelManager.getModel(uuid) : null;
        bedrockSkins$applyArmorOffset(matrices, model, stack, slot);
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void afterRenderArmor(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
        bedrockSkins$resetMatrix(matrices);
    }
    //?}
}