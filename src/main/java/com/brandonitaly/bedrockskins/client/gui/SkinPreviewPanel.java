package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;

public class SkinPreviewPanel {

    private final Minecraft minecraft;
    private final Font font;
    private final Runnable onFavoritesChanged;
    
    // State
    private int x, y, width, height;
    private FavoriteHeartButton favoriteButton;
    private Button selectButton;
    private LoadedSkin selectedSkin;
    private String currentSkinKey; // Track currently previewing skin key
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();

    public SkinPreviewPanel(Minecraft minecraft, Font font, Runnable onFavoritesChanged) {
        this.minecraft = minecraft;
        this.font = font;
        this.onFavoritesChanged = onFavoritesChanged;
    }
    
    public LoadedSkin getSelectedSkin() {
        return selectedSkin;
    }

    /**
     * @param widgetAdder A consumer to register buttons with the parent screen (e.g. this::addRenderableWidget)
     */
    public void init(int x, int y, int w, int h, Consumer<AbstractWidget> widgetAdder) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        
        int PANEL_PADDING = 4;
        int btnW = Math.min(width - 16, 140);
        int btnH = 20;
        int btnX = x + (width / 2) - (btnW / 2);
        int startY = y + h - PANEL_PADDING - btnH - 4;
        
        // Reset button (bottom)
        widgetAdder.accept(Button.builder(Component.translatable("bedrockskins.button.reset"), b -> resetSkin())
                .bounds(btnX, startY, btnW, btnH).build());
        startY -= (btnH + 4);
        
        // Select button (middle) - adjusted width to make room for favorite button
        int selectBtnW = btnW - 20 - 2; // 20px for heart button, 2px gap
        int selectBtnX = btnX + 20 + 2; // Offset to the right
        selectButton = Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin())
                .bounds(selectBtnX, startY, selectBtnW, btnH).build();
        widgetAdder.accept(selectButton);
        
        // Favorite button (left of select button) - heart icon button
        Identifier heartEmpty = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
        Identifier heartFull = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
        
        favoriteButton = new FavoriteHeartButton(btnX, startY, 20, heartEmpty, heartFull, b -> toggleFavorite());
        widgetAdder.accept(favoriteButton.getButton());
        
        initPreviewState();
    }

    public void initPreviewState() {
        if (this.selectedSkin != null) {
            updatePreviewModel(this.dummyUuid, this.selectedSkin.getKey());
            return;
        }

        String currentKey = SkinManager.getLocalSelectedKey();
        
        if (currentKey != null && !currentKey.isEmpty()) {
            this.dummyUuid = UUID.randomUUID();
            this.currentSkinKey = currentKey;
            
            // Try to load the skin object for the current key
            LoadedSkin currentSkin = SkinPackLoader.loadedSkins.get(currentKey);
            if (currentSkin != null) {
                this.selectedSkin = currentSkin;
            }
            
            updatePreviewModel(dummyUuid, currentKey);
            updateFavoriteButton(); // Enable buttons for current skin
        } else {
            if (minecraft.player != null) {
                this.dummyUuid = minecraft.player.getUUID();
            } else {
                this.dummyUuid = UUID.randomUUID();
            }
            this.currentSkinKey = null;
            updatePreviewModel(dummyUuid, null);
            updateFavoriteButton();
        }
    }

    public void setSelectedSkin(LoadedSkin skin) {
        this.selectedSkin = skin;
        this.currentSkinKey = skin != null ? skin.getKey() : null;
        updateFavoriteButton();
        if (skin != null) {
            if (minecraft.player != null && this.dummyUuid.equals(minecraft.player.getUUID())) {
                safeResetPreview(this.dummyUuid.toString());
                this.dummyUuid = UUID.randomUUID();
            }
            updatePreviewModel(dummyUuid, skin.getKey());
        }
    }

    private void updatePreviewModel(UUID uuid, String skinKey) {
        if (minecraft.level == null) return;
        
        if (!this.dummyUuid.equals(uuid)) {
            safeResetPreview(this.dummyUuid.toString());
        }
        this.dummyUuid = uuid;
        
        if (skinKey != null) {
            String[] parts = skinKey.split(":", 2);
            if (parts.length == 2) {
                SkinManager.setPreviewSkin(uuid.toString(), parts[0], parts[1]);
                safeRegisterTexture(skinKey);
            }
        }
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        GameProfile profile = new GameProfile(uuid, name);
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(minecraft.level, profile);

        // --- CAPE LOGIC (Mojang Mappings 1.21) ---
        Identifier capeToUse = null;
        
        // Priority 1: Use the skin pack's cape if it has one
        if (selectedSkin != null && selectedSkin.capeIdentifier != null) {
            capeToUse = selectedSkin.capeIdentifier;
        }
        // Priority 2: Use player's current cape if they have cape rendering enabled
        else if (minecraft.player != null && 
                 minecraft.player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE)) {
            PlayerSkin skin = minecraft.player.getSkin();
            capeToUse = skin.cape() != null ? skin.cape().id() : null;
        }
        
        if (capeToUse != null) {
            dummyPlayer.setForcedCape(capeToUse);
        }
    }

    private void applySkin() {
        if (selectedSkin == null) return;
        try {
            byte[] data = loadTextureData(selectedSkin);
            String key = selectedSkin.getKey();
            String[] parts = key.split(":", 2);
            String pack = parts.length == 2 ? parts[0] : "Remote";
            String name = parts.length == 2 ? parts[1] : key;
            
            safeRegisterTexture(key);
            
            if (minecraft.player != null) {
                SkinManager.setSkin(minecraft.player.getUUID().toString(), pack, name);
                if (data.length > 0) {
                    ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(
                        key, selectedSkin.getGeometryData().toString(), data
                    ));
                }
                
                String dispName = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
                if (dispName == null) dispName = selectedSkin.getSkinDisplayName();
                
                minecraft.player.displayClientMessage(
                    Component.translatable("bedrockskins.message.set_skin", dispName).withStyle(ChatFormatting.GREEN), 
                    true
                );
            } else {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), key);
                updatePreviewModel(dummyUuid, key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetSkin() {
        selectedSkin = null;
        currentSkinKey = null;
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID().toString());
            ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload("RESET", "", new byte[0]));
            minecraft.player.displayClientMessage(Component.translatable("bedrockskins.message.reset_default").withStyle(ChatFormatting.YELLOW), true);
            
            safeResetPreview(this.dummyUuid.toString());
            this.dummyUuid = minecraft.player.getUUID();
            updatePreviewModel(this.dummyUuid, null);
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
            safeResetPreview(dummyUuid.toString());
        }
        updateFavoriteButton();
    }

    private void toggleFavorite() {
        if (selectedSkin == null) return;
        if (FavoritesManager.isFavorite(selectedSkin)) FavoritesManager.removeFavorite(selectedSkin);
        else FavoritesManager.addFavorite(selectedSkin);
        
        updateFavoriteButton();
        if (onFavoritesChanged != null) onFavoritesChanged.run();
    }

    private void updateFavoriteButton() {
        if (favoriteButton == null) return;
        
        boolean hasSkin = currentSkinKey != null;
        boolean isFav = selectedSkin != null && FavoritesManager.isFavorite(selectedSkin);
        
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(hasSkin);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
        
        if (selectButton != null) {
            // Enable select button only when a different skin is selected
            selectButton.active = selectedSkin != null;
        }
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        drawPanel(gui, x, y, width, height, Component.translatable("bedrockskins.gui.preview"));
        
        int PANEL_HEADER_HEIGHT = 24;
        int entityY = y + PANEL_HEADER_HEIGHT;
        int buttonsHeight = 90;
        int entityH = height - PANEL_HEADER_HEIGHT - buttonsHeight;
        int availableHeight = Math.max(entityH, 50);
        int scale = Math.min((int)(availableHeight / 2.5), 80);
        float sensitivity = 0.25f;
        
        float centerX = x + width / 2.0f;
        float centerY = entityY + availableHeight / 2.0f + 20;
        float adjustedMouseX = centerX + (mouseX - centerX) * sensitivity;
        float adjustedMouseY = centerY + (mouseY - centerY) * sensitivity;

        if (dummyPlayer != null) {
            dummyPlayer.tickCount = (int)(Util.getMillis() / 50L);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                gui, 
                x + 5, entityY + 20, x + width - 5, entityY + availableHeight,
                scale, 0.0625f, 
                adjustedMouseX, adjustedMouseY, 
                dummyPlayer
            );
        } else {
            int textY = entityY + (availableHeight / 2) - (font.lineHeight / 2);
            gui.drawCenteredString(font, Component.translatable("bedrockskins.preview.unavailable"), (int)centerX, textY, 0xFFAAAAAA);
        }

        if (selectedSkin != null) {
            String name = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
            if (name == null) name = selectedSkin.getSkinDisplayName();
            int PANEL_PADDING = 4;
            int btnH = 20;
            int textY = y + height - PANEL_PADDING - (btnH * 2) - 8 - font.lineHeight - 4; // Above the buttons
            gui.drawCenteredString(font, name, (int)centerX, textY, 0xFFAAAAAA);
        }
    }
    
    public void renderSprites(GuiGraphics gui) {
        if (favoriteButton != null) {
            favoriteButton.renderSprites(gui);
        }
    }

    private void drawPanel(GuiGraphics gui, int x, int y, int w, int h, Component title) {
        int PANEL_HEADER_HEIGHT = 24;
        int right = x + w;
        int bottom = y + h;
        
        int COL_PANEL_BG = 0xE6181818;
        int COL_PANEL_HEADER = 0xFF252525;
        int COL_BORDER_OUTER = 0xFF000000;
        int COL_BORDER_INNER = 0xFF383838;
        int COL_TEXT_TITLE = 0xFFFFFFFF;

        gui.fill(x - 1, y - 1, right + 1, bottom + 1, COL_BORDER_OUTER);
        gui.fill(x, y, right, bottom, COL_PANEL_BG);
        gui.fill(x, y, right, y + PANEL_HEADER_HEIGHT, COL_PANEL_HEADER);
        gui.fill(x, y + PANEL_HEADER_HEIGHT, right, y + PANEL_HEADER_HEIGHT + 1, COL_BORDER_INNER);
        gui.drawCenteredString(font, title, x + (w / 2), y + 8, COL_TEXT_TITLE);
        
        gui.fill(x, y, right, y + 1, COL_BORDER_INNER); 
        gui.fill(x, bottom - 1, right, bottom, COL_BORDER_INNER); 
        gui.fill(x, y, x + 1, bottom, COL_BORDER_INNER); 
        gui.fill(right - 1, y, right, bottom, COL_BORDER_INNER);
    }

    private void safeResetPreview(String uuid) { try { SkinManager.resetPreviewSkin(uuid); } catch (Exception ignored) {} }
    private void safeRegisterTexture(String key) { try { SkinPackLoader.registerTextureFor(key); } catch (Exception ignored) {} }
    
    private byte[] loadTextureData(LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource res) {
                var resOpt = minecraft.getResourceManager().getResource(res.getId());
                if (resOpt.isPresent()) return resOpt.get().open().readAllBytes();
            } else if (src instanceof AssetSource.File f) {
                return Files.readAllBytes(new File(f.getPath()).toPath());
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }
    
    private static class FavoriteHeartButton {
        private final Button button;
        private final Identifier containerSprite;
        private final Identifier fullSprite;
        private boolean isFavorited = false;
        
        public FavoriteHeartButton(int x, int y, int size, Identifier containerSprite, Identifier fullSprite, Button.OnPress onPress) {
            this.containerSprite = containerSprite;
            this.fullSprite = fullSprite;
            this.button = Button.builder(Component.empty(), onPress)
                .size(size, size)
                .build();
            this.button.setPosition(x, y);
        }
        
        public AbstractWidget getButton() {
            return button;
        }
        
        public void setSelected(boolean selected) {
            this.isFavorited = selected;
        }
        
        public void setActive(boolean active) {
            button.active = active;
        }
        
        public void setTooltip(Component tooltip) {
            button.setTooltip(Tooltip.create(tooltip));
        }
        
        public void renderSprites(GuiGraphics graphics) {
            // Always render the container sprite (centered in 20x20 button)
            if (button.visible) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, containerSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                
                // Render the full heart on top if favorited
                if (isFavorited) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                }
            }
        }
    }
}
