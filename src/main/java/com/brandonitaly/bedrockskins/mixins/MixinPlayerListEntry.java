package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    public abstract GameProfile getProfile();

    //? if <=1.21.8 {
    /*@Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {*/
    //?} else {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
    //?}
        GameProfile profile = getProfile();
        java.util.UUID id = null;
        try {
            java.lang.reflect.Field f = profile.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Object o = f.get(profile);
            if (o instanceof java.util.UUID) id = (java.util.UUID) o;
            else if (o instanceof String) id = java.util.UUID.fromString((String) o);
        } catch (Exception ignored) {
            // fallback: try common getters
            try {
                java.lang.reflect.Method m = profile.getClass().getMethod("getId");
                Object o = m.invoke(profile);
                if (o instanceof java.util.UUID) id = (java.util.UUID) o;
                else if (o instanceof String) id = java.util.UUID.fromString((String) o);
            } catch (Exception ignored2) { }
        }
        if (id == null) return;
        String uuid = id.toString();
        String skinKey = SkinManager.getSkin(uuid);
        if (skinKey == null) return;
        var loadedSkin = SkinPackLoader.loadedSkins.get(skinKey);
        if (loadedSkin == null) return;

        if (loadedSkin.capeIdentifier != null) {
            PlayerSkin original = cir.getReturnValue();
            Identifier capeId = loadedSkin.capeIdentifier;
            
            // Define default Elytra ID
            Identifier defaultElytraId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");

            //? if <=1.21.8 {
            /*Identifier elytraId = original.elytraTexture() != null ? original.elytraTexture() : (original.capeTexture() != null ? original.capeTexture() : defaultElytraId);

            SkinTextures newTextures = new SkinTextures(
                original.texture(),
                original.textureUrl(),
                capeId,
                elytraId,
                original.model(),
                original.secure()
            );
            cir.setReturnValue(newTextures);*/
            //?} else {
            ClientAsset.Texture capeAsset = new ClientAsset.ResourceTexture(capeId, capeId);
            ClientAsset.Texture defaultElytraAsset = new ClientAsset.ResourceTexture(defaultElytraId, defaultElytraId);
            
            ClientAsset.Texture elytraAsset = original.elytra() != null ? original.elytra() : (original.cape() != null ? original.cape() : defaultElytraAsset);

            PlayerSkin newTextures = new PlayerSkin(
                original.body(),
                capeAsset,
                elytraAsset,
                original.model(),
                original.secure()
            );
            cir.setReturnValue(newTextures);
            //?}
        }
    }
}