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
        packList = SkinPackListWidget(client!!, packBounds.w, packBounds.h, packBounds.y, 24)
        packList!!.setX(packBounds.x)
        addDrawableChild(packList)

        // Grid Widget (Cell Height = 85 for player + text)
        skinGrid = SkinGridWidget(client!!, skinBounds.w, skinBounds.h, skinBounds.y, 90)
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
            
            packList!!.addEntryPublic(SkinPackEntry(packId, displayKey, fallback))
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
            skinGrid?.addEntryPublic(SkinRowEntry(rowSkins))
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

    // --- Custom Widgets ---

    inner class SkinPackListWidget(client: MinecraftClient, width: Int, height: Int, y: Int, itemHeight: Int) :
        AlwaysSelectedEntryListWidget<SkinPackEntry>(client, width, height, y, itemHeight) {
        override fun getRowWidth(): Int = width - 10
        override fun getScrollbarX(): Int = this.x + this.width - 6
        fun addEntryPublic(entry: SkinPackEntry) = super.addEntry(entry)
        fun clear() = super.clearEntries()
    }

    inner class SkinPackEntry(val packId: String, val translationKey: String, val fallbackName: String) : AlwaysSelectedEntryListWidget.Entry<SkinPackEntry>() {
        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            val isSelected = selectedPack == packId
            val color = if (isSelected) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()
            val translated = SkinPackLoader.getTranslation(translationKey) ?: fallbackName
            context.drawTextWithShadow(textRenderer, Text.literal(translated), getX() + 2, getY() + 6, color)
        }

        override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
            selectPack(packId)
            client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }
        override fun getNarration(): Text = Text.literal(packId)
    }

    // --- GRID SYSTEM ---

    inner class SkinGridWidget(client: MinecraftClient, width: Int, height: Int, y: Int, itemHeight: Int) :
        AlwaysSelectedEntryListWidget<SkinRowEntry>(client, width, height, y, itemHeight) {
        
        override fun getRowWidth(): Int = width - 10
        override fun getScrollbarX(): Int = this.x + this.width - 6

        override fun drawSelectionHighlight(context: DrawContext, entry: SkinRowEntry, color: Int) {
        }
        
        fun addEntryPublic(entry: SkinRowEntry) = super.addEntry(entry)
        
        fun clear() {
            // Must clean up the dummy entities created in the cells
            children().forEach { row -> row.cleanup() }
            super.clearEntries()
        }
    }

    inner class SkinRowEntry(private val skins: List<LoadedSkin>) : AlwaysSelectedEntryListWidget.Entry<SkinRowEntry>() {
        private val cells = mutableListOf<SkinCell>()

        init {
            // Initialize a display cell for each skin in this row
            skins.forEach { skin -> cells.add(SkinCell(skin)) }
        }

        fun cleanup() {
            cells.forEach { it.cleanup() }
        }

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            val startX = getX()
            val startY = getY()
            val cellWidth = 60
            val cellHeight = 85
            val padding = 5

            cells.forEachIndexed { index, cell ->
                val x = startX + (index * (cellWidth + padding))
                val isHovered = mouseX >= x && mouseX < x + cellWidth && mouseY >= startY && mouseY < startY + cellHeight
                cell.render(context, x, startY, cellWidth, cellHeight, isHovered, tickDelta)
            }
        }

        override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
            val startX = getX()
            val cellWidth = 60
            val padding = 5
            val localX = click.x - startX
            if (localX < 0) return false
            
            val index = (localX / (cellWidth + padding)).toInt()
            if (index in cells.indices) {
                val cellStart = index * (cellWidth + padding)
                if (localX >= cellStart && localX <= cellStart + cellWidth) {
                     val cell = cells[index]
                     selectSkin(cell.skin)
                     client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                     if (doubled) applySkin()
                     return true
                }
            }
            return false
        }
        
        override fun getNarration(): Text = Text.empty()
    }

    inner class SkinCell(val skin: LoadedSkin) {
        private var player: PreviewPlayer? = null
        private val uuid: UUID = UUID.randomUUID()
        private val name: String 

        init {
            val translated = SkinPackLoader.getTranslation(skin.safeSkinName) ?: skin.skinDisplayName
            name = translated
            
            val world = client?.world
            if (world != null) {
                val key = skin.key
                val parts = key.split(":", limit = 2)
                if (parts.size == 2) {
                    try {
                        SkinPackLoader.registerTextureFor(key)
                        SkinManager.setPreviewSkin(uuid.toString(), parts[0], parts[1])
                        player = PreviewPlayer(world, GameProfile(uuid, ""))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun cleanup() {
            try { SkinManager.resetPreviewSkin(uuid.toString()) } catch (_: Exception) {}
        }

        fun render(context: DrawContext, x: Int, y: Int, w: Int, h: Int, hovered: Boolean, delta: Float) {
            val isSelected = selectedSkin == skin
            
            val borderColor = if (isSelected) 0xFFFFFF00.toInt() else if (hovered) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            val bgColor = if (isSelected) 0x80555555.toInt() else 0x40000000.toInt()
            
            context.fill(x, y, x + w, y + h, bgColor)
            drawBorder(context, x, y, w, h, borderColor)

            // Render Tiny Entity
            if (player != null) {
                val scale = 30
                // Calculate the center of the render area
                val pX = (x + w / 2).toFloat()
                val pY = (y + h / 2).toFloat() 
                InventoryScreen.drawEntity(context, x + 2, y + 2, x + w - 2, y + h - 4, scale, 0.0625f, pX, pY, player!!)
            }
        }

        private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
            context.fill(x, y, x + width, y + 1, color) // Top
            context.fill(x, y + height - 1, x + width, y + height, color) // Bottom
            context.fill(x, y + 1, x + 1, y + height - 1, color) // Left
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color) // Right
        }
    }

    class PreviewPlayer(world: ClientWorld, profile: GameProfile) : OtherClientPlayerEntity(world, profile) {
        init { dataTracker.set(PlayerEntity.PLAYER_MODE_CUSTOMIZATION_ID, 127.toByte()) }
    }
}