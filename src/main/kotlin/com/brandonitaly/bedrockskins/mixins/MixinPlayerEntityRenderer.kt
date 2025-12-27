package com.brandonitaly.bedrockskins.mixins

import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.PlayerLikeEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import com.brandonitaly.bedrockskins.client.BedrockModelManager
import com.brandonitaly.bedrockskins.client.BedrockSkinState
import com.brandonitaly.bedrockskins.client.SkinManager
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.OverlayTexture

@Mixin(PlayerEntityRenderer::class)
abstract class MixinPlayerEntityRenderer {

    @Inject(method = ["updateRenderState"], at = [At("RETURN")])
    fun updateRenderState(
        player: PlayerLikeEntity,
        state: PlayerEntityRenderState,
        tickDelta: Float,
        ci: CallbackInfo
    ) {
        if (player is AbstractClientPlayerEntity) {
            (state as? BedrockSkinState)?.uniqueId = player.uuid
        }
    }

    @Inject(method = ["getTexture"], at = [At("HEAD")], cancellable = true)
    fun getTexture(state: PlayerEntityRenderState, ci: CallbackInfoReturnable<Identifier>) {
        val uuid = (state as? BedrockSkinState)?.uniqueId
        if (uuid != null) {
            val skinKey = SkinManager.getSkin(uuid.toString())
            if (skinKey != null) {
                val skin = SkinPackLoader.loadedSkins[skinKey]
                if (skin?.identifier != null) {
                    ci.returnValue = skin.identifier
                }
            }
        }
    }

    @Inject(method = ["renderRightArm"], at = [At("HEAD")], cancellable = true)
    fun renderRightArm(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        skinTexture: Identifier,
        sleeveVisible: Boolean,
        ci: CallbackInfo
    ) {
        val player = MinecraftClient.getInstance().player ?: return
        val uuid = player.uuid
        val bedrockModel = BedrockModelManager.getModel(uuid)

        if (bedrockModel != null) {
            var partKey = "right_arm"
            var part = bedrockModel.partsMap[partKey]
            if (part == null) {
                partKey = "rightArm"
                part = bedrockModel.partsMap[partKey]
            }

            var sleeveKey = "right_sleeve"
            var sleeve = bedrockModel.partsMap[sleeveKey]
            if (sleeve == null) {
                sleeveKey = "rightSleeve"
                sleeve = bedrockModel.partsMap[sleeveKey]
            }
            
            if (part != null) {
                // Resolve the correct texture
                val skinKey = SkinManager.getSkin(uuid.toString())
                val bedrockSkin = if (skinKey != null) SkinPackLoader.loadedSkins[skinKey] else null
                val texture = bedrockSkin?.identifier ?: skinTexture

                val layer = RenderLayers.entityTranslucent(texture)
                
                queue.submitCustom(matrices, layer) { entry, consumer ->
                    val ms = MatrixStack()
                    ms.peek().getPositionMatrix().set(entry.getPositionMatrix())
                    ms.peek().getNormalMatrix().set(entry.getNormalMatrix())
                    
                    // Reset rotations and positions to prevent animation artifacts in first person
                    part.resetTransform()
                    sleeve?.resetTransform()
                    
                    part.render(ms, consumer, light, OverlayTexture.DEFAULT_UV)
                    
                    val sleeveIsChild = part.hasChild("right_sleeve") || part.hasChild("rightSleeve")
                    if (sleeve != null && !sleeveIsChild) {
                        sleeve.render(ms, consumer, light, OverlayTexture.DEFAULT_UV)
                    }
                }
                ci.cancel()
            }
        }
    }

    @Inject(method = ["renderLeftArm"], at = [At("HEAD")], cancellable = true)
    fun renderLeftArm(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        skinTexture: Identifier,
        sleeveVisible: Boolean,
        ci: CallbackInfo
    ) {
        val player = MinecraftClient.getInstance().player ?: return
        val uuid = player.uuid
        val bedrockModel = BedrockModelManager.getModel(uuid)

        if (bedrockModel != null) {
            var partKey = "left_arm"
            var part = bedrockModel.partsMap[partKey]
            if (part == null) {
                partKey = "leftArm"
                part = bedrockModel.partsMap[partKey]
            }

            var sleeveKey = "left_sleeve"
            var sleeve = bedrockModel.partsMap[sleeveKey]
            if (sleeve == null) {
                sleeveKey = "leftSleeve"
                sleeve = bedrockModel.partsMap[sleeveKey]
            }
            
            if (part != null) {
                // Resolve the correct texture
                val skinKey = SkinManager.getSkin(uuid.toString())
                val bedrockSkin = if (skinKey != null) SkinPackLoader.loadedSkins[skinKey] else null
                val texture = bedrockSkin?.identifier ?: skinTexture

                val layer = RenderLayers.entityTranslucent(texture)
                
                queue.submitCustom(matrices, layer) { entry, consumer ->
                    val ms = MatrixStack()
                    ms.peek().getPositionMatrix().set(entry.getPositionMatrix())
                    ms.peek().getNormalMatrix().set(entry.getNormalMatrix())
                    
                    // Reset rotations and positions to prevent animation artifacts in first person
                    part.resetTransform()
                    sleeve?.resetTransform()
                    
                    part.render(ms, consumer, light, OverlayTexture.DEFAULT_UV)
                    
                    val sleeveIsChild = part.hasChild("left_sleeve") || part.hasChild("leftSleeve")
                    if (sleeve != null && !sleeveIsChild) {
                        sleeve.render(ms, consumer, light, OverlayTexture.DEFAULT_UV)
                    }
                }
                ci.cancel()
            }
        }
    }
}
