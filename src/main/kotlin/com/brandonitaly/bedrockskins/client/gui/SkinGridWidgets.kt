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

    override fun drawSelectionHighlight(context: DrawContext, entry: SkinRowEntry, color: Int) {}

    fun addEntryPublic(entry: SkinRowEntry) = super.addEntry(entry)

    fun addSkinsRow(skins: List<LoadedSkin>) {
        addEntryPublic(SkinRowEntry(skins))
    }

    fun clear() {
        children().forEach { row -> row.cleanup() }
        super.clearEntries()
    }

    inner class SkinRowEntry(private val skins: List<LoadedSkin>) : AlwaysSelectedEntryListWidget.Entry<SkinRowEntry>() {
        private val cells = mutableListOf<SkinCell>()

        init { skins.forEach { skin -> cells.add(SkinCell(skin)) } }

        fun cleanup() { cells.forEach { it.cleanup() } }

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            val startX = getX()
            val startY = getY()
            val cellWidth = CELL_WIDTH
            val cellHeight = CELL_HEIGHT
            val padding = CELL_PADDING

            cells.forEachIndexed { index, cell ->
                val x = startX + (index * (cellWidth + padding))
                val isHovered = mouseX >= x && mouseX < x + cellWidth && mouseY >= startY && mouseY < startY + cellHeight
                cell.render(context, x, startY, cellWidth, cellHeight, isHovered, tickDelta, mouseX, mouseY)
            }
        }

        override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
            val startX = getX()
            val cellWidth = CELL_WIDTH
            val padding = CELL_PADDING
            val localX = click.x - startX
            if (localX < 0) return false

            val index = (localX / (cellWidth + padding)).toInt()
            if (index in cells.indices) {
                val cellStart = index * (cellWidth + padding)
                if (localX >= cellStart && localX <= cellStart + cellWidth) {
                    val cell = cells[index]
                    onSelectSkin(cell.skin)
                    val client = MinecraftClient.getInstance()
                    client.soundManager?.play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f))
                    if (doubled) onSelectSkin(cell.skin)
                    return true
                }
            }
            return false
        }

        override fun getNarration(): Text = Text.empty()

        inner class SkinCell(val skin: LoadedSkin) {
            private var player: PreviewPlayer? = null
            private val uuid: UUID = UUID.randomUUID()
            private val name: String

            init {
                name = SkinPackLoader.getTranslation(skin.safeSkinName) ?: skin.skinDisplayName
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

                if (player != null) {
                    val scale = 30
                    val pX = (x + w / 2).toFloat()
                    val pY = (y + h / 2).toFloat()
                    InventoryScreen.drawEntity(context, x + 2, y + 2, x + w - 2, y + h - 4, scale, 0.0625f, pX, pY, player!!)
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
