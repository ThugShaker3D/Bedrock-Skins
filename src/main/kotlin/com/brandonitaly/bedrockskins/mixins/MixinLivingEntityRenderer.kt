package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.client.BedrockModelManager
import com.brandonitaly.bedrockskins.client.BedrockSkinState
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.state.CameraRenderState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntityRenderer::class)
abstract class MixinLivingEntityRenderer {

    @Shadow
    public lateinit var model: EntityModel<LivingEntityRenderState>

    @Unique
    private var originalModel: EntityModel<LivingEntityRenderState>? = null

    @Inject(method = ["render"], at = [At("HEAD")])
    fun onRenderHead(
        state: LivingEntityRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        camera: CameraRenderState,
        ci: CallbackInfo
    ) {
        if (state is PlayerEntityRenderState) {
            val skinState = state as? BedrockSkinState
            val uuid = skinState?.uniqueId
            if (uuid != null) {
                val bedrockModel = BedrockModelManager.getModel(uuid)
                
                if (bedrockModel != null) {
                    originalModel = this.model
                    @Suppress("UNCHECKED_CAST")
                    this.model = bedrockModel as EntityModel<LivingEntityRenderState>
                    
                    if (originalModel is net.minecraft.client.render.entity.model.PlayerEntityModel) {
                         bedrockModel.copyFromVanilla(originalModel as net.minecraft.client.render.entity.model.PlayerEntityModel)
                    }
                }
            }
        }
    }

    @Inject(method = ["render"], at = [At("RETURN")])
    fun onRenderReturn(
        state: LivingEntityRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        camera: CameraRenderState,
        ci: CallbackInfo
    ) {
        if (originalModel != null) {
            this.model = originalModel!!
            originalModel = null
        }
    }
}
