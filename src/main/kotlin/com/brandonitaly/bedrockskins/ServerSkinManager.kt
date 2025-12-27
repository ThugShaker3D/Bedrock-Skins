package com.brandonitaly.bedrockskins

import java.util.UUID

data class PlayerSkinData(
    val skinKey: String,
    val geometry: String,
    val textureData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerSkinData

        if (skinKey != other.skinKey) return false
        if (geometry != other.geometry) return false
        if (!textureData.contentEquals(other.textureData)) return false

        return true
    }
}

object ServerSkinManager {
    private val playerSkins = mutableMapOf<UUID, PlayerSkinData>()

    fun setSkin(uuid: UUID, data: PlayerSkinData) {
        playerSkins[uuid] = data
    }

    fun getSkin(uuid: UUID): PlayerSkinData? {
        return playerSkins[uuid]
    }

    fun removeSkin(uuid: UUID) {
        playerSkins.remove(uuid)
    }

    fun getAllSkins(): Map<UUID, PlayerSkinData> {
        return playerSkins.toMap()
    }
}
