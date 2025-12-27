package com.brandonitaly.bedrockskins

import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.util.Identifier
import net.minecraft.util.Uuids
import java.util.UUID
import net.minecraft.network.RegistryByteBuf

object BedrockSkinsNetworking {
    // Update skin for a player
    data class SkinUpdatePayload(
        val uuid: UUID, 
        val skinKey: String,
        val geometry: String,
        val textureData: ByteArray
    ) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<SkinUpdatePayload>(Identifier.of("bedrockskins", "skin_update"))
            val CODEC: PacketCodec<RegistryByteBuf, SkinUpdatePayload> = PacketCodec.tuple(
                Uuids.PACKET_CODEC, SkinUpdatePayload::uuid,
                PacketCodecs.string(32767), SkinUpdatePayload::skinKey,
                PacketCodecs.string(262144), SkinUpdatePayload::geometry,
                PacketCodecs.byteArray(1048576), SkinUpdatePayload::textureData,
                ::SkinUpdatePayload
            )
        }
        override fun getId() = ID
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SkinUpdatePayload
            if (uuid != other.uuid) return false
            if (skinKey != other.skinKey) return false
            if (geometry != other.geometry) return false
            if (!textureData.contentEquals(other.textureData)) return false
            return true
        }
    }

    // Set own skin
    data class SetSkinPayload(
        val skinKey: String,
        val geometry: String,
        val textureData: ByteArray
    ) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<SetSkinPayload>(Identifier.of("bedrockskins", "set_skin"))
            val CODEC: PacketCodec<RegistryByteBuf, SetSkinPayload> = PacketCodec.tuple(
                PacketCodecs.string(32767), SetSkinPayload::skinKey,
                PacketCodecs.string(262144), SetSkinPayload::geometry,
                PacketCodecs.byteArray(1048576), SetSkinPayload::textureData,
                ::SetSkinPayload
            )
        }
        override fun getId() = ID

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SetSkinPayload
            if (skinKey != other.skinKey) return false
            if (geometry != other.geometry) return false
            if (!textureData.contentEquals(other.textureData)) return false
            return true
        }
    }
}
