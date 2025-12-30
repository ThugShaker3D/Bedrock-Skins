package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.UUID;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

@Mixin(AvatarRenderState.class)
public class MixinPlayerEntityRenderState implements BedrockSkinState {
    @Unique
    private String bedrockSkinKey;
    @Unique
    private UUID uniqueId;

    @Override
    public String getBedrockSkinKey() {
        return bedrockSkinKey;
    }

    @Override
    public void setBedrockSkinKey(String key) {
        this.bedrockSkinKey = key;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(UUID uuid) {
        this.uniqueId = uuid;
    }
}
