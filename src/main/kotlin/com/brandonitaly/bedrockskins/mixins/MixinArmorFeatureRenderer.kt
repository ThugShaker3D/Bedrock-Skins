package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.client.BedrockModelManager
import com.brandonitaly.bedrockskins.client.BedrockSkinState
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer
import net.minecraft.client.render.entity.state.BipedEntityRenderState
import net.minecraft.item.ItemStack
import net.minecraft.entity.EquipmentSlot
import net.minecraft.client.util.math.MatrixStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ArmorFeatureRenderer::class)
abstract class MixinArmorFeatureRenderer {
    @Unique
    private val pushed = ThreadLocal<Boolean>()

    @Inject(method = ["renderArmor"], at = [At("HEAD")])
    fun beforeRenderArmor(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        stack: ItemStack,
        slot: EquipmentSlot,
        light: Int,
        state: BipedEntityRenderState,
        ci: CallbackInfo
    ) {
        val uuid = (state as? BedrockSkinState)?.uniqueId

        if (uuid != null) {
            val model = BedrockModelManager.getModel(uuid)
            if (model != null) {
                val pixels = when (slot) {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST -> model.upperArmorYOffset
                    // EquipmentSlot.LEGS, EquipmentSlot.FEET -> model.lowerArmorYOffset
                    else -> 0f
                }

                if (pixels != 0f) {
                    matrices.push()
                    val translateY = pixels * 0.0625f
                    matrices.translate(0.0, translateY.toDouble(), 0.0)
                    pushed.set(true)
                }
            }
        }
    }

    @Inject(method = ["renderArmor"], at = [At("RETURN")])
    fun afterRenderArmor(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        stack: ItemStack,
        slot: EquipmentSlot,
        light: Int,
        state: BipedEntityRenderState,
        ci: CallbackInfo
    ) {
        if (pushed.get() == true) {
            try { matrices.pop() } catch (_: Exception) { }
            pushed.remove()
        }
    }
}
