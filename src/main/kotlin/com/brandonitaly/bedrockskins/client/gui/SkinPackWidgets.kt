package com.brandonitaly.bedrockskins.client.gui

import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.text.Text

class SkinPackListWidget(
    client: MinecraftClient,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int,
    private val onSelect: (String) -> Unit,
    private val isSelected: (String) -> Boolean,
    private val textRenderer: TextRenderer
) : AlwaysSelectedEntryListWidget<SkinPackEntry>(client, width, height, y, itemHeight) {
    override fun getRowWidth(): Int = width - 10
    override fun getScrollbarX(): Int = this.x + this.width - 6
    fun addEntryPublic(entry: SkinPackEntry) = super.addEntry(entry)
    fun clear() = super.clearEntries()
}

class SkinPackEntry(
    val packId: String,
    val translationKey: String,
    val fallbackName: String,
    private val onSelect: (String) -> Unit,
    private val isSelectedFn: () -> Boolean,
    private val textRenderer: TextRenderer
) : AlwaysSelectedEntryListWidget.Entry<SkinPackEntry>() {
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
        val isSelected = isSelectedFn()
        val color = if (isSelected) 0xFFFFFF00.toInt() else if (hovered) 0xFFFFFFA0.toInt() else 0xFFFFFFFF.toInt()
        val translated = SkinPackLoader.getTranslation(translationKey) ?: fallbackName
        context.drawTextWithShadow(textRenderer, Text.literal(translated), getX() + 2, getY() + 6, color)
    }

    override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
        onSelect(packId)
        val client = MinecraftClient.getInstance()
        client.soundManager?.play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f))
        return true
    }

    override fun getNarration(): Text = Text.literal(packId)
}
