package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.client.BedrockSkinState
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import java.util.UUID

@Mixin(PlayerEntityRenderState::class)
class MixinPlayerEntityRenderState : BedrockSkinState {
    @Unique
    override var bedrockSkinKey: String? = null
    @Unique
    override var uniqueId: UUID? = null
}
