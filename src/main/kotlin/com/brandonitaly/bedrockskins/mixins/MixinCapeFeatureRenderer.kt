package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.client.BedrockModelManager
import com.brandonitaly.bedrockskins.client.BedrockSkinState
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(CapeFeatureRenderer::class)
abstract class MixinCapeFeatureRenderer {
    @Unique
    private val pushed = ThreadLocal<Boolean>()

    @Inject(method = ["render"], at = [At("HEAD")])
    fun beforeRender(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        state: PlayerEntityRenderState,
        limbAngle: Float,
        limbDistance: Float,
        ci: CallbackInfo
    ) {
        val uuid = (state as? BedrockSkinState)?.uniqueId

        if (uuid != null) {
            val model = BedrockModelManager.getModel(uuid)
            if (model != null && model.capeYOffset != 0f) {
                matrices.push()
                val translateY = model.capeYOffset * 0.0625f
                matrices.translate(0.0, translateY.toDouble(), 0.0)
                pushed.set(true)
            }
        }
    }

    @Inject(method = ["render"], at = [At("RETURN")])
    fun afterRender(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        state: PlayerEntityRenderState,
        limbAngle: Float,
        limbDistance: Float,
        ci: CallbackInfo
    ) {
        if (pushed.get() == true) {
            try { matrices.pop() } catch (_: Exception) { }
            pushed.remove()
        }
    }
}
