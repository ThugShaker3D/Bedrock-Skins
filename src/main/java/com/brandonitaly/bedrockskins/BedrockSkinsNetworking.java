package com.brandonitaly.bedrockskins;

import java.util.Arrays;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}

    public static final class SkinUpdatePayload implements CustomPacketPayload {
        //? if >=1.21.11 {
        public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_update"));
        //?} else {
        /*public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bedrockskins", "skin_update"));*/
        //?}
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinUpdatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinUpdatePayload::getUuid,
            ByteBufCodecs.stringUtf8(32767), SkinUpdatePayload::getSkinKey,
            ByteBufCodecs.stringUtf8(262144), SkinUpdatePayload::getGeometry,
            ByteBufCodecs.byteArray(1048576), SkinUpdatePayload::getTextureData,
            SkinUpdatePayload::new
        );

        private final java.util.UUID uuid;
        private final String skinKey;
        private final String geometry;
        private final byte[] textureData;

        public SkinUpdatePayload(java.util.UUID uuid, String skinKey, String geometry, byte[] textureData) {
            this.uuid = uuid;
            this.skinKey = skinKey;
            this.geometry = geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<?> type() {
            return ID;
        }

        public java.util.UUID getUuid() { return uuid; }
        public String getSkinKey() { return skinKey; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SkinUpdatePayload that = (SkinUpdatePayload) other;
            if (!uuid.equals(that.uuid)) return false;
            if (!skinKey.equals(that.skinKey)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = uuid.hashCode();
            result = 31 * result + skinKey.hashCode();
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }

    public static final class SetSkinPayload implements CustomPacketPayload {
        //? if >=1.21.11 {
        public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "set_skin"));
        //?} else {
        /*public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bedrockskins", "set_skin"));*/
        //?}
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSkinPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32767), SetSkinPayload::getSkinKey,
            ByteBufCodecs.stringUtf8(262144), SetSkinPayload::getGeometry,
            ByteBufCodecs.byteArray(1048576), SetSkinPayload::getTextureData,
            SetSkinPayload::new
        );

        private final String skinKey;
        private final String geometry;
        private final byte[] textureData;

        public SetSkinPayload(String skinKey, String geometry, byte[] textureData) {
            this.skinKey = skinKey;
            this.geometry = geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<?> type() { return ID; }

        public String getSkinKey() { return skinKey; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SetSkinPayload that = (SetSkinPayload) other;
            if (!skinKey.equals(that.skinKey)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = skinKey.hashCode();
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }
}
