package com.brandonitaly.bedrockskins.mixins

import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import net.minecraft.client.resource.language.TranslationStorage
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(TranslationStorage::class)
class MixinTranslationStorage {
    @Inject(method = ["get"], at = [At("HEAD")], cancellable = true)
    private fun onGet(key: String, fallback: String, cir: CallbackInfoReturnable<String>) {
        val custom = SkinPackLoader.getTranslation(key)
        if (custom != null) {
            cir.returnValue = custom
        }
    }
}
