package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking
import com.brandonitaly.bedrockskins.pack.AssetSource
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import java.io.File

class BedrockSkinsClient : ClientModInitializer {

    override fun onInitializeClient() {
        registerKeyBinding()
        registerLifecycleEvents()
        registerNetworking()
    }

    private fun registerKeyBinding() {
        try {
            val keyId = "key.bedrockskins.open"
            //? if <=1.21.8 {
            /*val category = "bedrockskins.controls"*/
            //?} else {
            val category = KeyBinding.Category.create(Identifier.of("bedrockskins", "controls"))
            //?}
            
            // Note: In 1.21+ you might need KeyBinding.Category.create, but strings often work depending on mapping
            val key = KeyBinding(keyId, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category)
            val openKey = KeyBindingHelper.registerKeyBinding(key)

            ClientTickEvents.END_CLIENT_TICK.register { client ->
                while (openKey.wasPressed()) {
                    client.setScreen(SkinSelectionScreen(client.currentScreen))
                }
            }
            println("BedrockSkinsClient: Registered keybinding (K)")
        } catch (e: Exception) {
            println("BedrockSkinsClient: Keybinding error: $e")
        }
    }

    private fun registerLifecycleEvents() {
        // Initial Load
        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            reloadResources(client)
            
            // Register Reload Listener
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                object : IdentifiableResourceReloadListener, SynchronousResourceReloader {
                    override fun getFabricId() = Identifier.of("bedrockskins", "reloader")
                    override fun reload(manager: ResourceManager) = reloadResources(MinecraftClient.getInstance())
                }
            )
        }

        // Apply Saved Skin on Join
        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            applySavedSkinOnJoin(client)
        }
    }

    private fun reloadResources(client: MinecraftClient) {
        try {
            SkinPackLoader.loadPacks()
            FavoritesManager.load()
            SkinManager.load()
            BedrockModelManager.clearAllModels()
            
            // Refresh current player skin if applicable
            client.player?.let { player ->
                SkinManager.getSkin(player.uuid.toString())?.let { key ->
                    val (pack, name) = splitKey(key)
                    SkinManager.setSkin(player.uuid.toString(), pack, name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySavedSkinOnJoin(client: MinecraftClient) {
        try {
            val state = StateManager.readState()
            val savedKey = state.selected ?: return
            val player = client.player ?: return
            
            // 1. Set Local Skin
            val (pack, name) = splitKey(savedKey)
            SkinManager.setSkin(player.uuid.toString(), pack, name)

            // 2. Sync to Server
            val loadedSkin = SkinPackLoader.loadedSkins[savedKey]
            if (loadedSkin != null) {
                val textureData = loadTextureData(client, loadedSkin)
                if (textureData.isNotEmpty()) {
                    ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload(savedKey, loadedSkin.geometryData.toString(), textureData))
                    println("BedrockSkinsClient: Synced skin $savedKey")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID) { payload, context ->
            val (uuid, key, geom, tex) = payload
            context.client().execute {
                if (key == "RESET") {
                    SkinManager.resetSkin(uuid.toString())
                } else {
                    SkinPackLoader.registerRemoteSkin(key, geom, tex)
                    val (pack, name) = splitKey(key)
                    SkinManager.setSkin(uuid.toString(), pack, name)
                }
            }
        }
    }

    // --- Helpers ---

    private fun splitKey(key: String): Pair<String, String> {
        val parts = key.split(":", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1] else "Remote" to key
    }

    private fun loadTextureData(client: MinecraftClient, skin: com.brandonitaly.bedrockskins.pack.LoadedSkin): ByteArray {
        return try {
            when (val src = skin.texture) {
                is AssetSource.Resource -> client.resourceManager.getResource(src.id).orElse(null)?.inputStream?.readBytes()
                is AssetSource.File -> File(src.path).readBytes()
                else -> ByteArray(0)
            } ?: ByteArray(0)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}