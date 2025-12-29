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
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.math.roundToInt

class SkinSelectionScreen(private val parent: Screen?) : Screen(Text.translatable("bedrockskins.gui.title")) {
    
    // UI State
    private var packList: SkinPackListWidget? = null
    private var skinGrid: SkinGridWidget? = null
    private var favoriteButton: ButtonWidget? = null
    private var selectedPack: String? = null
    private var selectedSkin: LoadedSkin? = null

    // Preview State
    private var dummyPlayer: OtherClientPlayerEntity? = null
    private var dummyUuid = UUID.randomUUID()
    
    // Layout
    private data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun cx() = x + w / 2
        fun cy() = y + h / 2
    }
    private var packBounds = Rect(0, 0, 0, 0)
    private var skinBounds = Rect(0, 0, 0, 0)
    private var previewBounds = Rect(0, 0, 0, 0)

    override fun init() {
        super.init()
        FavoritesManager.load()

        setupLayout()
        setupWidgets()
        
        // Initialize preview with current state
        val currentKey = SkinManager.getLocalSelectedKey()
        val currentPlayer = client?.player
        
        if (!currentKey.isNullOrEmpty()) {
             updatePreviewModel(UUID.randomUUID(), currentKey)
        } else if (currentPlayer != null) {
             updatePreviewModel(currentPlayer.uuid, null) // Null key = default player skin
        } else {
             updatePreviewModel(UUID.randomUUID(), null)
        }

        refreshPackList()
    }

    private fun setupLayout() {
        val top = 32
        val bottom = height - 40
        val contentH = bottom - top
        val availableW = width - 16
        
        val sideW = (availableW * 0.20).roundToInt().coerceIn(100, 160)
        val centerW = availableW - (sideW * 2) - 8

        packBounds = Rect(8, top, sideW, contentH)
        skinBounds = Rect(packBounds.x + packBounds.w + 4, top, centerW, contentH)
        previewBounds = Rect(skinBounds.x + skinBounds.w + 4, top, sideW, contentH)
    }

    private fun setupWidgets() {
        val mc = client ?: return

        // Pack List
        packList = SkinPackListWidget(mc, packBounds.w, packBounds.h, packBounds.y, 24,
            { id -> selectPack(id) },
            { id -> selectedPack == id },
            textRenderer
        ).apply { setX(packBounds.x) }
        addDrawableChild(packList)

        // Skin Grid
        skinGrid = SkinGridWidget(mc, skinBounds.w, skinBounds.h, skinBounds.y, 90,
            { skin -> selectSkin(skin) },
            { selectedSkin },
            textRenderer,
            { key -> safeRegisterTexture(key) },
            { uuid, pack, skin -> SkinManager.setPreviewSkin(uuid, pack, skin) },
            { uuid -> safeResetPreview(uuid) }
        ).apply { setX(skinBounds.x) }
        addDrawableChild(skinGrid)

        setupButtons()
    }

    private fun setupButtons() {
        val btnW = (previewBounds.w - 10).coerceAtMost(140)
        val btnX = previewBounds.cx() - btnW / 2
        var btnY = previewBounds.y + previewBounds.h - 24

        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.reset")) { resetSkin() }
            .dimensions(btnX, btnY, btnW, 20).build())
        
        btnY -= 24
        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.select")) { applySkin() }
            .dimensions(btnX, btnY, btnW, 20).build())
            
        btnY -= 24
        favoriteButton = ButtonWidget.builder(Text.translatable("bedrockskins.button.favorite")) { toggleFavorite() }
            .dimensions(btnX, btnY, btnW, 20).build().apply { active = false }
        addDrawableChild(favoriteButton)

        // Footer Buttons
        val footW = 150
        val footGap = 8
        val footX = width / 2 - (footW * 2 + footGap) / 2
        
        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.open_packs")) { openSkinPacksFolder() }
            .dimensions(footX, height - 28, footW, 20).build())
            
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done")) { close() }
            .dimensions(footX + footW + footGap, height - 28, footW, 20).build())
    }

    // --- Logic & State Management ---

    private fun updatePreviewModel(uuid: UUID, skinKey: String?) {
        val mc = client ?: return
        val world = mc.world ?: return
        
        // Clean up old
        safeResetPreview(dummyUuid.toString())
        dummyUuid = uuid

        // Register new texture if needed
        if (skinKey != null) {
            val parts = skinKey.split(":", limit = 2)
            if (parts.size == 2) {
                SkinManager.setPreviewSkin(uuid.toString(), parts[0], parts[1])
                safeRegisterTexture(skinKey)
            }
        }

        // Create Entity
        val name = mc.player?.name?.string ?: "Preview"
        val profile = GameProfile(uuid, name)
        dummyPlayer = PreviewPlayer(world, profile)
    }

    private fun selectPack(packId: String) {
        selectedPack = packId
        skinGrid?.clear()
        skinGrid?.setScrollY(0.0)

        val skins = if (packId == "skinpack.Favorites") {
            FavoritesManager.getFavoriteKeys().mapNotNull { SkinPackLoader.loadedSkins[it] }
        } else {
            SkinPackLoader.loadedSkins.values.filter { it.id == packId }
        }

        // Auto-calculate columns
        val cols = ((skinBounds.w - 20) / 65).coerceAtLeast(1)
        skins.chunked(cols).forEach { skinGrid?.addSkinsRow(it) }
    }

    private fun selectSkin(skin: LoadedSkin) {
        selectedSkin = skin
        updateFavoriteButton()
        if (skin.key.isNotEmpty()) {
            updatePreviewModel(UUID.randomUUID(), skin.key)
        }
    }

    private fun applySkin() {
        val skin = selectedSkin ?: return
        if (skin.key.isEmpty()) return
        val player = client?.player

        try {
            val textureData = loadTextureData(skin)
            val parts = skin.key.split(":", limit = 2)
            val pack = if (parts.size == 2) parts[0] else "Remote"
            val name = if (parts.size == 2) parts[1] else skin.key

            safeRegisterTexture(skin.key)

            if (player != null) {
                // In-Game: Send to Server
                SkinManager.setSkin(player.uuid.toString(), pack, name)
                if (textureData.isNotEmpty()) {
                    ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload(skin.key, skin.geometryData.toString(), textureData))
                }
                val tName = SkinPackLoader.getTranslation(skin.safeSkinName) ?: skin.skinDisplayName
                player.sendMessage(Text.translatable("bedrockskins.message.set_skin", tName).formatted(Formatting.GREEN), true)
            } else {
                // Main Menu: Save State
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), skin.key)
                updatePreviewModel(dummyUuid, skin.key) // visual refresh
            }
        } catch (e: Exception) {
            e.printStackTrace()
            player?.sendMessage(Text.literal("Error: ${e.message}").formatted(Formatting.RED), false)
        }
    }

    private fun resetSkin() {
        selectedSkin = null
        val player = client?.player
        
        if (player != null) {
            SkinManager.resetSkin(player.uuid.toString())
            ClientPlayNetworking.send(BedrockSkinsNetworking.SetSkinPayload("RESET", "", ByteArray(0)))
            player.sendMessage(Text.translatable("bedrockskins.message.reset_default").formatted(Formatting.YELLOW), true)
            updatePreviewModel(player.uuid, null)
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null)
            safeResetPreview(dummyUuid.toString())
        }
    }

    // --- Helpers ---

    private fun refreshPackList() {
        packList?.clear()
        val packs = SkinPackLoader.loadedSkins.values.groupBy { it.id }.toMutableMap()
        if (FavoritesManager.getFavoriteKeys().isNotEmpty()) packs["skinpack.Favorites"] = emptyList()

        // Sort: Favorites first, then by defined order, then alphabetical
        val sorted = packs.keys.sortedWith(Comparator { k1, k2 ->
            if (k1 == "skinpack.Favorites") return@Comparator -1
            if (k2 == "skinpack.Favorites") return@Comparator 1
            val i1 = SkinPackLoader.packOrder.indexOf(k1)
            val i2 = SkinPackLoader.packOrder.indexOf(k2)
            if (i1 != -1 && i2 != -1) i1.compareTo(i2) else if (i1 != -1) -1 else if (i2 != -1) 1 else k1.compareTo(k2)
        })

        sorted.forEach { pid ->
            val s = packs[pid]?.firstOrNull()
            val name = if (pid == "skinpack.Favorites") "Favorites" else (s?.packDisplayName ?: pid)
            val disp = s?.safePackName ?: pid
            packList?.addEntryPublic(SkinPackEntry(pid, disp, name, { selectPack(it) }, { selectedPack == pid }, textRenderer))
        }
    }

    private fun loadTextureData(skin: LoadedSkin): ByteArray {
        return when (val src = skin.texture) {
            is AssetSource.Resource -> MinecraftClient.getInstance().resourceManager.getResource(src.id).orElseThrow().inputStream.readBytes()
            is AssetSource.File -> Files.readAllBytes(File(src.path).toPath())
            else -> ByteArray(0)
        }
    }

    private fun toggleFavorite() {
        selectedSkin?.let { skin ->
            if (FavoritesManager.isFavorite(skin)) FavoritesManager.removeFavorite(skin) else FavoritesManager.addFavorite(skin)
            updateFavoriteButton()
            refreshPackList()
            if (selectedPack == "skinpack.Favorites") selectPack("skinpack.Favorites")
        }
    }

    private fun updateFavoriteButton() {
        favoriteButton?.active = selectedSkin != null
        val isFav = selectedSkin?.let { FavoritesManager.isFavorite(it) } ?: false
        favoriteButton?.message = Text.translatable(if (isFav) "bedrockskins.button.unfavorite" else "bedrockskins.button.favorite")
    }

    private fun openSkinPacksFolder() {
        val dir = File(client?.runDirectory, "skin_packs").apply { if (!exists()) mkdirs() }
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir)
            else Util.getOperatingSystem().open(dir)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun safeRegisterTexture(key: String) = try { SkinPackLoader.registerTextureFor(key) } catch (e: Exception) { e.printStackTrace() }
    private fun safeResetPreview(uuid: String) = try { SkinManager.resetPreviewSkin(uuid) } catch (_: Exception) {}

    // --- Rendering ---

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Backgrounds
        val bg = 0x40000000
        context.fill(packBounds.x, packBounds.y, packBounds.x + packBounds.w, packBounds.y + packBounds.h, bg)
        context.fill(skinBounds.x, skinBounds.y, skinBounds.x + skinBounds.w, skinBounds.y + skinBounds.h, bg)
        context.fill(previewBounds.x, previewBounds.y, previewBounds.x + previewBounds.w, previewBounds.y + previewBounds.h, bg)

        // Headers
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.packs"), packBounds.cx(), packBounds.y - 12, -1)
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.skins"), skinBounds.cx(), skinBounds.y - 12, -1)
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.gui.preview"), previewBounds.cx(), previewBounds.y - 12, -1)

        // Selected Name
        selectedSkin?.let {
            val name = SkinPackLoader.getTranslation(it.safeSkinName) ?: it.skinDisplayName
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(name), previewBounds.cx(), previewBounds.y + 8, -1)
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
        safeResetPreview(dummyUuid.toString())
        client?.setScreen(parent)
    }
}