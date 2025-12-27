package com.brandonitaly.bedrockskins

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import org.slf4j.LoggerFactory

class BedrockSkins : ModInitializer {
    private val logger = LoggerFactory.getLogger("bedrockskins")
    private val lastSkinChange = mutableMapOf<java.util.UUID, Long>()

    override fun onInitialize() {
        logger.info("Initializing Bedrock Skins Mod")

        // Register Payloads
        PayloadTypeRegistry.playS2C().register(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC)

        // Handle player joining - send them all existing skins
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            ServerSkinManager.getAllSkins().forEach { (uuid, skinData) ->
                ServerPlayNetworking.send(handler.player, BedrockSkinsNetworking.SkinUpdatePayload(
                    uuid, 
                    skinData.skinKey,
                    skinData.geometry,
                    skinData.textureData
                ))
            }
        }

        // Handle client setting their skin
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetSkinPayload.ID) { payload, context ->
            val skinKey = payload.skinKey
            val player = context.player()
            val uuid = player.uuid
            val server = context.server()
            
            val geometry = payload.geometry
            val textureData = payload.textureData

            server.execute {
                // Security: Rate Limiting / cooldown (5s)
                val now = System.currentTimeMillis()
                val last = lastSkinChange[uuid]
                if (last != null && now - last < 5_000L) {
                    logger.warn("Player ${player.name.string} is changing skins too quickly.")
                    return@execute
                }

                // Security: Server-side Validation
                if (skinKey != "RESET") {
                    // 1. Check Texture Size (512KB limit)
                    if (textureData.size > 512 * 1024) {
                        logger.warn("Player ${player.name.string} sent oversized texture (${textureData.size} bytes).")
                        return@execute
                    }

                    // 2. Check PNG Magic Bytes
                    if (textureData.size < 8 ||
                        textureData[0] != 0x89.toByte() ||
                        textureData[1] != 0x50.toByte() ||
                        textureData[2] != 0x4E.toByte() ||
                        textureData[3] != 0x47.toByte()
                    ) {
                        logger.warn("Player ${player.name.string} sent invalid texture format (not PNG).")
                        return@execute
                    }

                    // 3. Check Geometry Size (100KB limit)
                    if (geometry.length > 100_000) {
                        logger.warn("Player ${player.name.string} sent oversized geometry (${geometry.length} chars).")
                        return@execute
                    }
                }

                logger.info("Player ${player.name.string} set skin to $skinKey")
                
                if (skinKey == "RESET") {
                    ServerSkinManager.removeSkin(uuid)
                } else {
                    val data = PlayerSkinData(skinKey, geometry, textureData)
                    ServerSkinManager.setSkin(uuid, data)
                }

                // Broadcast to all players
                val updatePayload = BedrockSkinsNetworking.SkinUpdatePayload(uuid, skinKey, geometry, textureData)
                
                server.playerManager.playerList.forEach { p ->
                    ServerPlayNetworking.send(p, updatePayload)
                }
            }
        }
    }
}
