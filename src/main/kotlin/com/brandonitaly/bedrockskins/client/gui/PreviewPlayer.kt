package com.brandonitaly.bedrockskins.client.gui

import com.mojang.authlib.GameProfile
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import java.util.UUID

class PreviewPlayer(world: ClientWorld, profile: GameProfile) : OtherClientPlayerEntity(world, profile) {
    //? if <=1.21.8 {
    /*init { dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, 127.toByte()) }*/
    //?} else {
    init { dataTracker.set(PlayerEntity.PLAYER_MODE_CUSTOMIZATION_ID, 127.toByte()) }
    //?}
} 

object PreviewPlayerPool {
    private val pool = mutableMapOf<UUID, PreviewPlayer>()

    fun get(world: ClientWorld, profile: GameProfile): PreviewPlayer {
        val id = profile.id
        return pool.getOrPut(id) { PreviewPlayer(world, profile) }
    }

    fun remove(id: UUID) {
        pool.remove(id)
    }

    fun clear() {
        pool.clear()
    }
}
