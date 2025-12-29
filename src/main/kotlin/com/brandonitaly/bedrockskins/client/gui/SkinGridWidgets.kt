package com.brandonitaly.bedrockskins.client.gui

import com.brandonitaly.bedrockskins.client.SkinManager
import com.brandonitaly.bedrockskins.pack.LoadedSkin
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.Text
import java.util.UUID

class SkinGridWidget(
    client: MinecraftClient,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int,
    private val onSelectSkin: (LoadedSkin) -> Unit,
    private val getSelectedSkin: () -> LoadedSkin?,
    private val textRenderer: TextRenderer,
    private val registerTextureFor: (String) -> Unit,
    private val setPreviewSkin: (uuid: String, pack: String, skin: String) -> Unit,
    private val resetPreviewSkin: (uuid: String) -> Unit
) : AlwaysSelectedEntryListWidget<SkinGridWidget.SkinRowEntry>(client, width, height, y, itemHeight) {

    override fun getRowWidth(): Int = width - 10
    override fun getScrollbarX(): Int = this.x + this.width - 6

    //? if <=1.21.8 {
    /*override fun drawSelectionHighlight(context: DrawContext, startX: Int, startY: Int, width: Int, height: Int, color: Int) {}*/
    //?} else {
    override fun drawSelectionHighlight(context: DrawContext, entry: SkinRowEntry, color: Int) {}
    //?}

    fun addEntryPublic(entry: SkinRowEntry) = super.addEntry(entry)
    fun addSkinsRow(skins: List<LoadedSkin>) = addEntryPublic(SkinRowEntry(skins))

    fun clear() {
        children().forEach { row -> row.cleanup() }
        super.clearEntries()
    }

    inner class SkinRowEntry(private val skins: List<LoadedSkin>) : AlwaysSelectedEntryListWidget.Entry<SkinRowEntry>() {
        private val cells = mutableListOf<SkinCell>()

        init { skins.forEach { skin -> cells.add(SkinCell(skin)) } }

        fun cleanup() { cells.forEach { it.cleanup() } }

        // --- Shared Logic ---

        private fun renderCommon(context: DrawContext, x: Int, y: Int, mouseX: Int, mouseY: Int, tickDelta: Float) {
            cells.forEachIndexed { i, cell ->
                val cx = x + (i * (CELL_WIDTH + CELL_PADDING))
                val isHovered = mouseX >= cx && mouseX < cx + CELL_WIDTH && mouseY >= y && mouseY < y + CELL_HEIGHT
                cell.render(context, cx, y, CELL_WIDTH, CELL_HEIGHT, isHovered, tickDelta, mouseX, mouseY)
            }
        }

        private fun clickCommon(localX: Int, doubled: Boolean): Boolean {
            if (localX < 0) return false
            val index = localX / (CELL_WIDTH + CELL_PADDING)
            
            if (index in cells.indices) {
                val cellStart = index * (CELL_WIDTH + CELL_PADDING)
                if (localX >= cellStart && localX <= cellStart + CELL_WIDTH) {
                    val cell = cells[index]
                    onSelectSkin(cell.skin)
                    MinecraftClient.getInstance().soundManager?.play(
                        net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f)
                    )
                    if (doubled) onSelectSkin(cell.skin)
                    return true
                }
            }
            return false
        }

        // --- Version Specific Wrappers ---

        //? if <=1.21.8 {
        /*
        override fun render(context: DrawContext, index: Int, y: Int, x: Int, entryWidth: Int, entryHeight: Int, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            renderCommon(context, x, y, mouseX, mouseY, tickDelta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return clickCommon((mouseX - getX()).toInt(), false)
        }
        */
        //?} else {
        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            renderCommon(context, getX(), getY(), mouseX, mouseY, tickDelta)
        }

        override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
            return clickCommon((click.x - getX()).toInt(), doubled)
        }
        //?}

        override fun getNarration(): Text = Text.empty()

        inner class SkinCell(val skin: LoadedSkin) {
            private var player: PreviewPlayer? = null
            private val uuid: UUID = UUID.randomUUID()
            private val name: String = SkinPackLoader.getTranslation(skin.safeSkinName) ?: skin.skinDisplayName

            init {
                val world: ClientWorld? = MinecraftClient.getInstance().world
                if (world != null) {
                    val parts = skin.key.split(":", limit = 2)
                    if (parts.size == 2) {
                        try {
                            registerTextureFor(skin.key)
                            setPreviewSkin(uuid.toString(), parts[0], parts[1])
                            player = PreviewPlayerPool.get(world, com.mojang.authlib.GameProfile(uuid, ""))
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }

            fun cleanup() { try { resetPreviewSkin(uuid.toString()) } catch (_: Exception) {} ; PreviewPlayerPool.remove(uuid) }

            fun render(context: DrawContext, x: Int, y: Int, w: Int, h: Int, hovered: Boolean, delta: Float, mouseX: Int, mouseY: Int) {
                val isSelected = getSelectedSkin() == skin
                val borderColor = if (isSelected) 0xFFFFFF00.toInt() else if (hovered) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                val bgColor = if (isSelected) 0x80555555.toInt() else 0x40000000.toInt()

                context.fill(x, y, x + w, y + h, bgColor)
                drawBorder(context, x, y, w, h, borderColor)

                player?.let {
                    val pX = (x + w / 2).toFloat()
                    val pY = (y + h / 2).toFloat()
                    // Draw entity with simplified args
                    InventoryScreen.drawEntity(context, x + 2, y + 2, x + w - 2, y + h - 4, 30, 0.0625f, pX, pY, it)
                }

                if (hovered) context.drawTooltip(textRenderer, Text.literal(name), mouseX, mouseY)
            }

            private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
                context.fill(x, y, x + width, y + 1, color)
                context.fill(x, y + height - 1, x + width, y + height, color)
                context.fill(x, y + 1, x + 1, y + height - 1, color)
                context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
            }
        }
    }

    companion object {
        const val CELL_WIDTH = 60
        const val CELL_HEIGHT = 85
        const val CELL_PADDING = 5
    }
}