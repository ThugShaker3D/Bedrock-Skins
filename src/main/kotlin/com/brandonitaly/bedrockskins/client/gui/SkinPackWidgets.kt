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

    // --- Shared Logic ---

    private fun renderCommon(context: DrawContext, x: Int, y: Int, hovered: Boolean) {
        val isSelected = isSelectedFn()
        val color = if (isSelected) 0xFFFFFF00.toInt() else if (hovered) 0xFFFFFFA0.toInt() else 0xFFFFFFFF.toInt()
        val translated = SkinPackLoader.getTranslation(translationKey) ?: fallbackName
        context.drawTextWithShadow(textRenderer, Text.literal(translated), x + 2, y + 6, color)
    }

    private fun clickCommon(): Boolean {
        onSelect(packId)
        MinecraftClient.getInstance().soundManager?.play(
            net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f)
        )
        return true
    }

    // --- Version Specific Wrappers ---

    //? if <=1.21.8 {
    /*
    override fun render(context: DrawContext, index: Int, y: Int, x: Int, entryWidth: Int, entryHeight: Int, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
        renderCommon(context, x, y, hovered)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return clickCommon()
    }
    */
    //?} else {
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
        renderCommon(context, getX(), getY(), hovered)
    }

    override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
        return clickCommon()
    }
    //?}

    override fun getNarration(): Text = Text.literal(packId)
}