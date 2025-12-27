package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking
import com.brandonitaly.bedrockskins.pack.AssetSource
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.text.Text
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import net.minecraft.resource.ResourceManager
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.resource.SynchronousResourceReloader
import java.io.File

class BedrockSkinsClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Register keybinding early so GameOptions can add the category before initialization
        try {
            val openKey = KeyBindingHelper.registerKeyBinding(
                KeyBinding(
                    "key.bedrockskins.open",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    KeyBinding.Category.create(Identifier.of("bedrockskins", "controls"))
                )
            )

            ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
                while (openKey.wasPressed()) {
                    client.execute {
                        client.setScreen(com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen(client.currentScreen))
                    }
                }
            })
            println("BedrockSkinsClient: Registered keybinding for skin selection (default K)")
        } catch (e: Exception) {
            println("BedrockSkinsClient: Failed to register keybinding: $e")
        }

        // Register textures when client is ready
        ClientLifecycleEvents.CLIENT_STARTED.register {
            // Load skin packs (metadata only) - moved here to ensure LanguageManager is ready
            SkinPackLoader.loadPacks()

            // Load favorites prior to any operation that may persist state
            try {
                FavoritesManager.load()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // SkinManager.load populates runtime mapping if player is present
            try {
                SkinManager.load()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Register a JOIN handler to apply saved selected skin when the client joins a world/server
            try {
                ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
                    try {
                        val state = StateManager.readState()
                        val savedKey = state.selected
                        if (!savedKey.isNullOrEmpty()) {
                            // Always set runtime mapping so the local player sees their selected skin immediately
                            val parts = savedKey.split(":", limit = 2)
                            if (client.player != null) {
                                if (parts.size >= 2) {
                                    SkinManager.setSkin(client.player.uuid.toString(), parts[0], savedKey.substring(parts[0].length + 1))
                                } else {
                                    SkinManager.setSkin(client.player.uuid.toString(), "Remote", savedKey)
                                }
                            } else {
                                // When no player object available, set preview mapping so UI reflects selection
                                if (parts.size >= 2) {
                                    SkinManager.setPreviewSkin("preview", parts[0], savedKey.substring(parts[0].length + 1))
                                } else {
                                    SkinManager.setPreviewSkin("preview", "Remote", savedKey)
                                }
                            }

                            // Try to send payload to server (server must support mod)
                            try {
                                val loadedSkin = SkinPackLoader.loadedSkins[savedKey]
                                if (loadedSkin != null && client.player != null) {
                                    val geometry = loadedSkin.geometryData.toString()
                                    
                                    val textureData: ByteArray = when (val source = loadedSkin.texture) {
                                        is AssetSource.Resource -> {
                                            client.resourceManager.getResource(source.id)
                                                .orElse(null)?.inputStream?.readBytes() ?: ByteArray(0)
                                        }
                                        is AssetSource.File -> {
                                            File(source.path).readBytes()
                                        }
                                        is AssetSource.Remote -> {
                                            // Remote skins usually don't need re-uploading this way, or we'd need to cache the bytes elsewhere
                                            ByteArray(0) 
                                        }
                                    }

                                    if (textureData.isNotEmpty()) {
                                        ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload(savedKey, geometry, textureData))
                                    }
                                    println("BedrockSkinsClient: Applied saved skin on join: $savedKey")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Attempt to register a resource reload listener so skin packs reload when resource packs change
            try {
                ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                    object : IdentifiableResourceReloadListener, SynchronousResourceReloader {
                        override fun getFabricId(): Identifier {
                            return Identifier.of("bedrockskins", "skinpack-reloader")
                        }

                        override fun reload(resourceManager: ResourceManager) {
                            try {
                                SkinPackLoader.loadPacks()
                                println("SkinPackLoader: Reloaded skin packs after resource reload")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
                println("BedrockSkinsClient: Registered resource reload listener")
            } catch (e: Exception) {
                println("BedrockSkinsClient: Failed to register resource reload listener: $e")
            }
        }

        // Register packet receiver
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID) { payload, context ->
            val uuid = payload.uuid
            val skinKey = payload.skinKey
            val geometry = payload.geometry
            val textureData = payload.textureData

            context.client().execute {
                if (skinKey == "RESET") {
                    SkinManager.resetSkin(uuid.toString())
                } else {
                    SkinPackLoader.registerRemoteSkin(skinKey, geometry, textureData)

                    val parts = skinKey.split(":")
                    if (parts.size >= 2) {
                        val packName = parts[0]
                        val skinName = skinKey.substring(packName.length + 1)
                        SkinManager.setSkin(uuid.toString(), packName, skinName)
                    } else {
                        SkinManager.setSkin(uuid.toString(), "Remote", skinKey)
                    }
                }
            }
        }
    }
}