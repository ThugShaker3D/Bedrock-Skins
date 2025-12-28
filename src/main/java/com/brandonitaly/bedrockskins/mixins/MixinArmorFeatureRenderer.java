package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinArmorFeatureRenderer {
    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    // Inject into the main render method to reset visibility to true by default.
    // This runs before the specific armor slots are checked/rendered.
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid != null) {
            var model = BedrockModelManager.getModel(uuid);
            if (model instanceof BedrockPlayerModel bedrockModel) {
                // Default to visible. Specific features will hide them if present.
                bedrockModel.setBedrockPartVisible("bodyArmor", true);
                bedrockModel.setBedrockPartVisible("helmet", true);
            }
        }
    }

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void beforeRenderArmor(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid != null) {
            var model = BedrockModelManager.getModel(uuid);
            
            // Hiding Logic
            if (model instanceof BedrockPlayerModel bedrockModel && !stack.isEmpty()) {
                if (slot == EquipmentSlot.CHEST) {
                    bedrockModel.setBedrockPartVisible("bodyArmor", false);
                } else if (slot == EquipmentSlot.HEAD) {
                    bedrockModel.setBedrockPartVisible("helmet", false);
                }
            }

            // Offset Logic
            if (model != null) {
                float pixels = 0f;
                switch (slot) {
                    case HEAD:
                    case CHEST:
                        pixels = model.upperArmorYOffset;
                        break;
                    default:
                        break;
                }
                if (pixels != 0f) {
                    matrices.push();
                    double translateY = pixels * 0.0625f;
                    matrices.translate(0.0, translateY, 0.0);
                    pushed.set(true);
                }
            }
        }
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void afterRenderArmor(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }
}