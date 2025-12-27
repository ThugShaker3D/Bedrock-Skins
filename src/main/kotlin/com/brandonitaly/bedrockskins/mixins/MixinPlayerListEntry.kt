package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.client.SkinManager
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import com.mojang.authlib.GameProfile
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.entity.player.SkinTextures
import net.minecraft.util.AssetInfo
import net.minecraft.util.Identifier
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlayerListEntry::class)
abstract class MixinPlayerListEntry {

    @Shadow
    abstract fun getProfile(): GameProfile

    @Inject(method = ["getSkinTextures"], at = [At("RETURN")], cancellable = true)
    private fun onGetSkinTextures(cir: CallbackInfoReturnable<SkinTextures>) {
        val profile = getProfile()
        val uuid = profile.id.toString()
        val skinKey = SkinManager.getSkin(uuid) ?: return
        val loadedSkin = SkinPackLoader.loadedSkins[skinKey] ?: return

        if (loadedSkin.capeIdentifier != null) {
            val original = cir.returnValue
            val capeId = loadedSkin.capeIdentifier!!
            
            // Create AssetInfo.TextureAsset for the cape
            val capeAsset: AssetInfo.TextureAsset = AssetInfo.TextureAssetInfo(capeId, capeId)

            // Preserve original elytra texture:
            // If explicit elytra exists, use it.
            // If not, check if a cape existed (which would have been used as elytra).
            // If neither, use the default elytra texture to prevent the Bedrock cape from being used as elytra.
            val defaultElytraId = Identifier.of("minecraft", "textures/entity/equipment/wings/elytra.png")
            val defaultElytraAsset = AssetInfo.TextureAssetInfo(defaultElytraId, defaultElytraId)
            
            val elytraAsset = original.elytra() ?: original.cape() ?: defaultElytraAsset

            val newTextures = SkinTextures(
                original.body(),
                capeAsset,
                elytraAsset,
                original.model(),
                original.secure()
            )
            cir.returnValue = newTextures
        }
    }
}
