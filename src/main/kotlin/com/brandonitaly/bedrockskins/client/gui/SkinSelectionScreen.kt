package com.brandonitaly.bedrockskins.client.gui

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking
import com.brandonitaly.bedrockskins.client.FavoritesManager
import com.brandonitaly.bedrockskins.client.SkinManager
import com.brandonitaly.bedrockskins.client.StateManager
import com.brandonitaly.bedrockskins.pack.AssetSource
import com.brandonitaly.bedrockskins.pack.LoadedSkin
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.math.roundToInt

class SkinSelectionScreen(private val parent: Screen?) : Screen(Text.translatable("bedrockskins.gui.title")) {
    private var packList: SkinPackListWidget? = null
    private var skinGrid: SkinGridWidget? = null

    private var selectedPack: String? = null
    private var selectedSkin: LoadedSkin? = null
    
    // Main preview player (right side)
    private var dummyPlayer: OtherClientPlayerEntity? = null
    private var dummyUuid = UUID.randomUUID()
    
    private var scrollAnchorMs: Long = 0L
    private var favoriteButton: ButtonWidget? = null

    // Layout Bounds
    private var packBounds = Rect(0, 0, 0, 0)
    private var skinBounds = Rect(0, 0, 0, 0)
    private var previewBounds = Rect(0, 0, 0, 0)
    private var contentTop = 0
    private var contentBottom = 0

    data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun cx() = x + w / 2
        fun cy() = y + h / 2
    }

    override fun init() {
        super.init()
        FavoritesManager.load()
        scrollAnchorMs = Util.getMeasuringTimeMs()

        setupMainPreviewPlayer()

        // --- Responsive Layout Logic ---
        val headerHeight = 32
        val footerHeight = 40
        val margin = 8
        val gap = 4

        contentTop = headerHeight
        contentBottom = height - footerHeight
        val contentHeight = contentBottom - contentTop
        val availableWidth = width - (margin * 2)

        val packW = (availableWidth * 0.20).roundToInt().coerceIn(100, 160)
        val previewW = (availableWidth * 0.20).roundToInt().coerceIn(100, 160)
        val skinW = availableWidth - packW - previewW - (gap * 2)

        packBounds = Rect(margin, contentTop, packW, contentHeight)
        skinBounds = Rect(packBounds.x + packBounds.w + gap, contentTop, skinW, contentHeight)
        previewBounds = Rect(skinBounds.x + skinBounds.w + gap, contentTop, previewW, contentHeight)

        // Lists
        packList = SkinPackListWidget(client!!, packBounds.w, packBounds.h, packBounds.y, 24,
            { id -> selectPack(id) },
            { id -> selectedPack == id },
            textRenderer
        )
        packList!!.setX(packBounds.x)
        addDrawableChild(packList)

        // Grid Widget (Cell Height = 85 for player + text)
        skinGrid = SkinGridWidget(
            client!!, skinBounds.w, skinBounds.h, skinBounds.y, 90,
            { skin -> selectSkin(skin) },
            { selectedSkin },
            textRenderer,
            { key -> try { SkinPackLoader.registerTextureFor(key) } catch (e: Exception) { e.printStackTrace() } },
            { uuid, pack, skinName -> SkinManager.setPreviewSkin(uuid, pack, skinName) },
            { uuid -> try { SkinManager.resetPreviewSkin(uuid) } catch (e: Exception) { e.printStackTrace() } }
        )
        skinGrid!!.setX(skinBounds.x)
        addDrawableChild(skinGrid)

        // Buttons
        setupButtons()

        refreshPackList()
    }

    private fun setupMainPreviewPlayer() {
        if (client?.world != null && dummyPlayer == null) {
            val player = client!!.player
            if (player != null) {
                val localSelected = SkinManager.getLocalSelectedKey()
                if (localSelected.isNullOrEmpty()) {
                    dummyUuid = player.uuid
                    val profile = GameProfile(player.uuid, player.name.string)
                    dummyPlayer = PreviewPlayer(client!!.world!!, profile)
                    val current = SkinManager.getSkin(player.uuid.toString())
                    if (!current.isNullOrEmpty()) {
                        try { SkinPackLoader.registerTextureFor(current) } catch (e: Exception) { e.printStackTrace() }
                    }
                } else {
                    val profile = GameProfile(dummyUuid, player.name.string)
                    dummyPlayer = PreviewPlayer(client!!.world!!, profile)
                    val parts = localSelected.split(":", limit = 2)
                    if (parts.size == 2) {
                        SkinManager.setPreviewSkin(dummyUuid.toString(), parts[0], parts[1])
                        try { SkinPackLoader.registerTextureFor(localSelected) } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            } else {
                val profile = GameProfile(dummyUuid, "Preview")
                dummyPlayer = PreviewPlayer(client!!.world!!, profile)
            }
        }
    }

    private fun setupButtons() {
        val buttonWidth = (previewBounds.w - 10).coerceAtMost(140)
        val buttonX = previewBounds.cx() - buttonWidth / 2
        var buttonY = previewBounds.y + previewBounds.h - 24

        val resetButton = ButtonWidget.builder(Text.translatable("bedrockskins.button.reset")) { resetSkin() }
            .dimensions(buttonX, buttonY, buttonWidth, 20).build()
        addDrawableChild(resetButton)
        buttonY -= 24

        val selectButton = ButtonWidget.builder(Text.translatable("bedrockskins.button.select")) { applySkin() }
            .dimensions(buttonX, buttonY, buttonWidth, 20).build()
        addDrawableChild(selectButton)
        buttonY -= 24

        favoriteButton = ButtonWidget.builder(Text.translatable("bedrockskins.button.favorite")) { toggleFavorite() }
            .dimensions(buttonX, buttonY, buttonWidth, 20).build()
        favoriteButton?.active = false
        addDrawableChild(favoriteButton)

        val bottomButtonWidth = 150
        val bottomGap = 8
        val totalWidth = bottomButtonWidth * 2 + bottomGap
        val startX = width / 2 - totalWidth / 2

        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.open_packs")) {
            openSkinPacksFolder()
        }.dimensions(startX, height - 28, bottomButtonWidth, 20).build())

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done")) { close() }
            .dimensions(startX + bottomButtonWidth + bottomGap, height - 28, bottomButtonWidth, 20).build())
    }

    private fun refreshPackList() {
        packList!!.clear()
        val skinsByPack = SkinPackLoader.loadedSkins.values.groupBy { it.id }.toMutableMap()
        
        if (FavoritesManager.getFavoriteKeys().isNotEmpty()) {
            skinsByPack["skinpack.Favorites"] = emptyList()
        }
        val orderList = SkinPackLoader.packOrder
        
        // Sort using the raw ID keys
        val sortedKeys = skinsByPack.keys.sortedWith { key1, key2 ->
            if (key1 == "skinpack.Favorites") -1 else if (key2 == "skinpack.Favorites") 1
            else {
                val i1 = orderList.indexOf(key1)
                val i2 = orderList.indexOf(key2)
                if (i1 != -1 && i2 != -1) i1.compareTo(i2) else if (i1 != -1) -1 else if (i2 != -1) 1 else key1.compareTo(key2)
            }
        }
        
        sortedKeys.forEach { packId ->
            val skin = skinsByPack[packId]?.firstOrNull()
            val displayKey = skin?.safePackName ?: packId
            val fallback = if (packId == "skinpack.Favorites") "Favorites" else (skin?.packDisplayName ?: packId)
            
            packList!!.addEntryPublic(SkinPackEntry(packId, displayKey, fallback, { id -> selectPack(id) }, { selectedPack == packId }, textRenderer))
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Draw backgrounds for areas
        context.fill(packBounds.x, packBounds.y, packBounds.x + packBounds.w, packBounds.y + packBounds.h, 0x40000000)
        context.fill(skinBounds.x, skinBounds.y, skinBounds.x + skinBounds.w, skinBounds.y + skinBounds.h, 0x40000000)
        context.fill(previewBounds.x, previewBounds.y, previewBounds.x + previewBounds.w, previewBounds.y + previewBounds.h, 0x40000000)

        // Draw Headers
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.packs"), packBounds.cx(), packBounds.y - 12, 0xFFFFFFFF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.skins"), skinBounds.cx(), skinBounds.y - 12, 0xFFFFFFFF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.preview"), previewBounds.cx(), previewBounds.y - 12, 0xFFFFFFFF.toInt())

        // Draw Selected Skin Name in the Preview Area
        if (selectedSkin != null) {
            val name = SkinPackLoader.getTranslation(selectedSkin!!.safeSkinName) ?: selectedSkin!!.skinDisplayName
            // Draw slightly down from the top of the preview box
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(name), previewBounds.cx(), previewBounds.y + 8, 0xFFFFFFFF.toInt())
        }

        super.render(context, mouseX, mouseY, delta)
        renderPreviewEntity(context, mouseX, mouseY)
    }

    private fun renderPreviewEntity(context: DrawContext, mouseX: Int, mouseY: Int) {
        val availableHeight = (previewBounds.h - 80).coerceAtLeast(100)
        val scale = (availableHeight / 2.5).toInt().coerceAtMost(80)
        val sensitivity = 0.25f
        val centerX = previewBounds.cx().toFloat()
        val centerY = (previewBounds.y + availableHeight / 2 + 10).toFloat()
        val adjustedMouseX = centerX + (mouseX - centerX) * sensitivity
        val adjustedMouseY = centerY + (mouseY - centerY) * sensitivity

        if (dummyPlayer != null) {
            dummyPlayer!!.age = (Util.getMeasuringTimeMs() / 50L).toInt()
            InventoryScreen.drawEntity(
                context, previewBounds.x, previewBounds.y, previewBounds.x + previewBounds.w, previewBounds.y + availableHeight,
                scale, 0.0625f, adjustedMouseX, adjustedMouseY, dummyPlayer!!
            )
        } else if (client?.player == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.preview.unavailable"), previewBounds.cx(), previewBounds.cy(), 0xFFAAAAAA.toInt())
        }
    }

    override fun close() {
        skinGrid?.clear()
        SkinManager.resetPreviewSkin(dummyUuid.toString())
        client?.setScreen(parent)
    }

    // --- Selection Logic ---

    private fun selectPack(packId: String) {
        selectedPack = packId
        skinGrid?.clear()
        skinGrid?.setScrollY(0.0)

        val skins = if (packId == "skinpack.Favorites") {
            FavoritesManager.getFavoriteKeys().mapNotNull { SkinPackLoader.loadedSkins[it] }
        } else {
            SkinPackLoader.loadedSkins.values.filter { it.id == packId }
        }

        // Calculate columns based on width and cell size
        val cellWidth = 60
        val padding = 5
        val availableW = skinBounds.w - 20 
        val columns = (availableW / (cellWidth + padding)).coerceAtLeast(1)

        // Chunk skins into rows
        skins.chunked(columns).forEach { rowSkins ->
            skinGrid?.addSkinsRow(rowSkins)
        }
    }

    private fun selectSkin(skin: LoadedSkin) {
        selectedSkin = skin
        updateFavoriteButton()

        val key = skin.key
        if (key.isNotEmpty()) {
            val parts = key.split(":", limit = 2)
            if (parts.size == 2) {
                try { SkinManager.resetPreviewSkin(dummyUuid.toString()) } catch (_: Exception) {}
                dummyUuid = UUID.randomUUID()
                if (client?.world != null) {
                    val profile = GameProfile(dummyUuid, client!!.player?.name?.string ?: "Preview")
                    dummyPlayer = PreviewPlayer(client!!.world!!, profile)
                }
                SkinManager.setPreviewSkin(dummyUuid.toString(), parts[0], parts[1])
                try { SkinPackLoader.registerTextureFor(key) } catch (e: Exception) { e.printStackTrace() }
                scrollAnchorMs = Util.getMeasuringTimeMs()
            }
        }
    }

    private fun updateFavoriteButton() {
        if (selectedSkin != null) {
            favoriteButton?.active = true
            val isFav = FavoritesManager.isFavorite(selectedSkin!!)
            favoriteButton?.message = Text.translatable(if (isFav) "bedrockskins.button.unfavorite" else "bedrockskins.button.favorite")
        } else {
            favoriteButton?.active = false
            favoriteButton?.message = Text.translatable("bedrockskins.button.favorite")
        }
    }

    private fun toggleFavorite() {
        val skin = selectedSkin ?: return
        if (FavoritesManager.isFavorite(skin)) FavoritesManager.removeFavorite(skin)
        else FavoritesManager.addFavorite(skin)
        updateFavoriteButton()
        refreshPackList()
        if (selectedPack == "skinpack.Favorites") selectPack("skinpack.Favorites")
    }

    private fun applySkin() {
        val player = client?.player
        val skin = selectedSkin ?: return
        val key = skin.key
        if (key.isEmpty()) return
        try {
            val geometry = skin.geometryData.toString()
            
            val textureData: ByteArray = when (val source = skin.texture) {
                is AssetSource.Resource -> {
                    MinecraftClient.getInstance().resourceManager.getResource(source.id)
                        .orElseThrow().inputStream.readBytes()
                }
                is AssetSource.File -> {
                    Files.readAllBytes(File(source.path).toPath())
                }
                is AssetSource.Remote -> ByteArray(0)
            }

            val parts = key.split(":", limit = 2)
            try { SkinPackLoader.registerTextureFor(key) } catch (e: Exception) { e.printStackTrace() }

            if (player != null) {
                if (parts.size == 2) SkinManager.setSkin(player.uuid.toString(), parts[0], parts[1])
                else SkinManager.setSkin(player.uuid.toString(), "Remote", key)
                
                if (textureData.isNotEmpty()) {
                    ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload(key, geometry, textureData))
                }
                
                val skinTranslation = SkinPackLoader.getTranslation(skin.safeSkinName) ?: skin.skinDisplayName
                player.sendMessage(Text.translatable("bedrockskins.message.set_skin", skinTranslation).formatted(Formatting.GREEN), true)
            } else {
                val favorites = FavoritesManager.getFavoriteKeys()
                StateManager.saveState(favorites, key)
                if (parts.size == 2) SkinManager.setPreviewSkin(dummyUuid.toString(), parts[0], parts[1])
                else SkinManager.setPreviewSkin(dummyUuid.toString(), "Remote", key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            player?.sendMessage(Text.literal("Error: ${e.message}").formatted(Formatting.RED), false)
        }
    }

    private fun resetSkin() {
        val player = client?.player
        selectedSkin = null
        try { SkinManager.resetPreviewSkin(dummyUuid.toString()) } catch (_: Exception) {}
        if (player != null) {
            SkinManager.resetSkin(player.uuid.toString())
            ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload("RESET", "", ByteArray(0)))
            player.sendMessage(Text.translatable("bedrockskins.message.reset_default").formatted(Formatting.YELLOW), true)
            dummyUuid = player.uuid
            if (client?.world != null) {
                val profile = GameProfile(player.uuid, player.name.string)
                dummyPlayer = PreviewPlayer(client!!.world!!, profile)
            }
        } else {
            val favorites = FavoritesManager.getFavoriteKeys()
            StateManager.saveState(favorites, null)
        }
    }
    
    private fun openSkinPacksFolder() {
        val client = MinecraftClient.getInstance()
        val dir = File(client.runDirectory, "skin_packs")
        try {
            if (!dir.exists()) dir.mkdirs()
            if (Desktop.isDesktopSupported()) { Desktop.getDesktop().open(dir); return }
            val os = System.getProperty("os.name").lowercase()
            val cmd = when {
                os.contains("win") -> arrayOf("explorer.exe", dir.absolutePath)
                os.contains("mac") -> arrayOf("open", dir.absolutePath)
                else -> arrayOf("xdg-open", dir.absolutePath)
            }
            Runtime.getRuntime().exec(cmd)
        } catch (e: Exception) { e.printStackTrace() }
    }
}