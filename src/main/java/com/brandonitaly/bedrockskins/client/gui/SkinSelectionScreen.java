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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

public class SkinSelectionScreen extends Screen {

    // --- Visual Constants ---
    private static final int PANEL_HEADER_HEIGHT = 24;
    private static final int PANEL_PADDING = 4;
    
    // Modern Dark Theme Colors (ARGB)
    private static final int COL_BG_GRADIENT_TOP = 0xC0101010;
    private static final int COL_BG_GRADIENT_BOT = 0xD0000000;
    private static final int COL_PANEL_BG = 0xE6181818; // High opacity dark gray
    private static final int COL_PANEL_HEADER = 0xFF252525;
    private static final int COL_BORDER_OUTER = 0xFF000000;
    private static final int COL_BORDER_INNER = 0xFF383838;
    private static final int COL_TEXT_TITLE = 0xFFFFFFFF;
    private static final int COL_TEXT_SEC = 0xFFAAAAAA;

    // --- Widgets & State ---
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private Button favoriteButton;
    
    private String selectedPackId;
    private LoadedSkin selectedSkin;

    // Preview
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();
    
    // Optimization: Cache skins by pack ID to avoid O(N) lookup on every click
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    // Layout
    private final Rect rPacks = new Rect();
    private final Rect rSkins = new Rect();
    private final Rect rPreview = new Rect();

    public SkinSelectionScreen(Screen parent) {
        super(Component.translatable("bedrockskins.gui.title"));
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        
        // 1. Build optimized cache
        buildSkinCache();

        // 2. Compute dynamic layout
        calculateLayout();

        // 3. Initialize UI components
        initWidgets();
        
        // 4. Setup preview entity
        initPreviewState();
    }
    
    /**
     * Optimization: Groups all loaded skins by their Pack ID into a map.
     * This makes switching packs instantaneous.
     */
    private void buildSkinCache() {
        skinCache.clear();
        
        // Group standard skins
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            skinCache.computeIfAbsent(skin.getId(), k -> new ArrayList<>()).add(skin);
        }
        
        // Build Favorites list
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin s = SkinPackLoader.loadedSkins.get(key);
            if (s != null) favs.add(s);
        }
        skinCache.put("skinpack.Favorites", favs);
    }

    private void calculateLayout() {
        int topNav = 32;
        int botNav = 32;
        int hMargin = 10;
        int gap = 6;
        
        int fullW = this.width - (hMargin * 2);
        int fullH = this.height - topNav - botNav;
        
        // Responsive Layout: 
        // Side panels get 22% of width, clamped between 130px and 200px.
        // Center panel takes the rest.
        int sideW = Math.max(130, Math.min(200, (int)(fullW * 0.22)));
        int centerW = fullW - (sideW * 2) - (gap * 2);
        
        // Safety for extremely small windows
        if (centerW < 100) {
            sideW = (fullW - 100 - (gap * 2)) / 2;
            centerW = 100;
        }

        rPacks.set(hMargin, topNav, sideW, fullH);
        rSkins.set(rPacks.right() + gap, topNav, centerW, fullH);
        rPreview.set(rSkins.right() + gap, topNav, sideW, fullH);
    }

    private void initWidgets() {
        if (minecraft == null) return;

        // -- Pack List --
        int plY = rPacks.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int plH = rPacks.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);
        
        packList = new SkinPackListWidget(minecraft, rPacks.w - (PANEL_PADDING * 2), plH, plY, 24,
                this::selectPack,
                id -> Objects.equals(selectedPackId, id),
                font);
        ReflectionHelper.setPos(packList, rPacks.x + PANEL_PADDING, plY);
        addRenderableWidget(packList);

        // -- Skin Grid --
        int sgY = rSkins.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int sgH = rSkins.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);
        
        skinGrid = new SkinGridWidget(minecraft, rSkins.w - (PANEL_PADDING * 2), sgH, sgY, 90,
                this::selectSkin,
                () -> selectedSkin,
                font,
                this::safeRegisterTexture,
                SkinManager::setPreviewSkin,
                this::safeResetPreview);
        ReflectionHelper.setPos(skinGrid, rSkins.x + PANEL_PADDING, sgY);
        addRenderableWidget(skinGrid);

        // -- Buttons --
        initPreviewButtons();
        initFooterButtons();
        
        // Load initial data
        refreshPackList();

        // FIX: Restore the grid content if a pack was previously selected.
        // On resize, init() is called again, creating a new empty skinGrid. 
        // We must manually refill it with the currently selected pack.
        if (selectedPackId != null) {
            selectPack(selectedPackId);
        }
    }
    
    private void initPreviewButtons() {
        int btnW = Math.min(rPreview.w - 16, 140);
        int btnH = 20;
        int btnX = rPreview.centerX() - (btnW / 2);
        int startY = rPreview.bottom() - PANEL_PADDING - btnH - 4;
        
        addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.reset"), b -> resetSkin())
                .bounds(btnX, startY, btnW, btnH).build());
        startY -= (btnH + 4);
        
        addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin())
                .bounds(btnX, startY, btnW, btnH).build());
        startY -= (btnH + 4);
        
        favoriteButton = Button.builder(Component.translatable("bedrockskins.button.favorite"), b -> toggleFavorite())
                .bounds(btnX, startY, btnW, btnH).build();
        favoriteButton.active = false;
        addRenderableWidget(favoriteButton);
    }

    private void initFooterButtons() {
        int btnW = 150;
        int gap = 10;
        int y = this.height - 24;
        int centerX = this.width / 2;
        
        addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder())
                .bounds(centerX - btnW - (gap/2), y, btnW, 20).build());
        
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(centerX + (gap/2), y, btnW, 20).build());
    }

    private void initPreviewState() {
        // Fix: If a skin is currently selected (e.g. during a window resize), preserve the preview for it.
        // This prevents the preview from reverting to the player's actual skin on resize.
        if (this.selectedSkin != null) {
            updatePreviewModel(this.dummyUuid, this.selectedSkin.getKey());
            return;
        }

        String currentKey = SkinManager.getLocalSelectedKey();
        
        // Logic: If the player has a custom skin set, use a dummy UUID to show it.
        // If they have no custom skin (vanilla), use their real UUID so the preview shows their actual current skin.
        if (currentKey != null && !currentKey.isEmpty()) {
            this.dummyUuid = UUID.randomUUID();
            updatePreviewModel(dummyUuid, currentKey);
        } else {
            if (minecraft.player != null) {
                this.dummyUuid = minecraft.player.getUUID();
            } else {
                this.dummyUuid = UUID.randomUUID();
            }
            // Passing null here ensures we don't override the texture, 
            // allowing the entity to render with its natural skin properties
            updatePreviewModel(dummyUuid, null);
        }
    }

    // --- Application Logic ---

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        // Sort Packs: Favorites -> Defined Order -> Alphabetical
        List<String> sortedPacks = new ArrayList<>(skinCache.keySet());
        sortedPacks.remove("skinpack.Favorites"); 
        
        sortedPacks.sort((k1, k2) -> {
            int i1 = SkinPackLoader.packOrder.indexOf(k1);
            int i2 = SkinPackLoader.packOrder.indexOf(k2);
            if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
            if (i1 != -1) return -1;
            if (i2 != -1) return 1;
            return k1.compareToIgnoreCase(k2);
        });

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) {
            sortedPacks.add(0, "skinpack.Favorites");
        }

        for (String pid : sortedPacks) {
            String display = pid;
            String internal = pid;
            
            // Resolve display name from cache (faster than searching all skins)
            List<LoadedSkin> skins = skinCache.get(pid);
            if (skins != null && !skins.isEmpty()) {
                LoadedSkin first = skins.get(0);
                display = first.getSafePackName();
                internal = first.getPackDisplayName();
            }
            
            if ("skinpack.Favorites".equals(pid)) {
                display = "Favorites";
                internal = "Favorites";
            }

            packList.addEntryPublic(new SkinPackListWidget.SkinPackEntry(
                    pid, display, internal,
                    this::selectPack,
                    () -> Objects.equals(selectedPackId, pid),
                    font
            ));
        }
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
        }

        List<LoadedSkin> skins = skinCache.getOrDefault(packId, Collections.emptyList());
        
        // Responsive columns based on actual width
        int itemWidth = 65;
        int totalWidth = rSkins.w - (PANEL_PADDING * 2) - 10;
        int cols = Math.max(1, totalWidth / itemWidth);
        
        for (int i = 0; i < skins.size(); i += cols) {
            List<LoadedSkin> row = skins.subList(i, Math.min(i + cols, skins.size()));
            skinGrid.addSkinsRow(row);
        }
    }

    private void selectSkin(LoadedSkin skin) {
        this.selectedSkin = skin;
        updateFavoriteButton();
        if (skin != null) {
            // Detach from real player UUID if we are currently using it.
            // This ensures we don't apply the preview texture to the real player entity.
            if (minecraft.player != null && this.dummyUuid.equals(minecraft.player.getUUID())) {
                // Reset the override on the real player before switching
                safeResetPreview(this.dummyUuid.toString());
                this.dummyUuid = UUID.randomUUID();
            }
            updatePreviewModel(dummyUuid, skin.getKey());
        }
    }

    private void updatePreviewModel(UUID uuid, String skinKey) {
        if (minecraft.level == null) return;
        
        // Clean up previous preview state
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
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID().toString());
            ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload("RESET", "", new byte[0]));
            minecraft.player.displayClientMessage(Component.translatable("bedrockskins.message.reset_default").withStyle(ChatFormatting.YELLOW), true);
            
            // Revert preview to the actual player UUID to show vanilla skin
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
        
        // Refresh cache specifically for favorites pack
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin s = SkinPackLoader.loadedSkins.get(key);
            if (s != null) favs.add(s);
        }
        skinCache.put("skinpack.Favorites", favs);
        
        updateFavoriteButton();
        refreshPackList();
        
        if ("skinpack.Favorites".equals(selectedPackId)) selectPack("skinpack.Favorites");
    }
    
    private void updateFavoriteButton() {
        if (favoriteButton == null) return;
        favoriteButton.active = selectedSkin != null;
        boolean isFav = selectedSkin != null && FavoritesManager.isFavorite(selectedSkin);
        favoriteButton.setMessage(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
    }

    // --- Helpers ---

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
    
    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, "skin_packs");
        if (!dir.exists()) dir.mkdirs();
        Util.getPlatform().openFile(dir);
    }

    private void safeRegisterTexture(String key) {
        try { SkinPackLoader.registerTextureFor(key); } catch (Exception ignored) {}
    }

    private void safeResetPreview(String uuid) {
        try { SkinManager.resetPreviewSkin(uuid); } catch (Exception ignored) {}
    }

    // --- Rendering ---

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // Title
        gui.drawCenteredString(font, this.title, this.width / 2, 12, COL_TEXT_TITLE);

        // Draw styled panels
        drawPanel(gui, rPacks, Component.translatable("bedrockskins.gui.packs"));
        drawPanel(gui, rSkins, Component.translatable("bedrockskins.gui.skins"));
        drawPanel(gui, rPreview, Component.translatable("bedrockskins.gui.preview"));

        // Render widgets (lists, buttons)
        super.render(gui, mouseX, mouseY, delta);
        
        // Render 3D Entity
        renderPreviewEntity(gui, mouseX, mouseY);
        
        // Render Skin Name
        if (selectedSkin != null) {
            String name = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
            if (name == null) name = selectedSkin.getSkinDisplayName();
            
            // Draw centered under the preview header
            int textY = rPreview.y + PANEL_HEADER_HEIGHT + 6;
            gui.drawCenteredString(font, name, rPreview.centerX(), textY, COL_TEXT_SEC);
        }
    }
    
    private void drawPanel(GuiGraphics gui, Rect r, Component title) {
        // 1. Shadow border
        gui.fill(r.x - 1, r.y - 1, r.right() + 1, r.bottom() + 1, COL_BORDER_OUTER);
        
        // 2. Main Background
        gui.fill(r.x, r.y, r.right(), r.bottom(), COL_PANEL_BG);
        
        // 3. Header Background
        gui.fill(r.x, r.y, r.right(), r.y + PANEL_HEADER_HEIGHT, COL_PANEL_HEADER);
        
        // 4. Separator Line
        gui.fill(r.x, r.y + PANEL_HEADER_HEIGHT, r.right(), r.y + PANEL_HEADER_HEIGHT + 1, COL_BORDER_INNER);
        
        // 5. Title
        gui.drawCenteredString(font, title, r.centerX(), r.y + 8, COL_TEXT_TITLE);
        
        // 6. Inner Border (1px highlight/depth)
        gui.fill(r.x, r.y, r.right(), r.y + 1, COL_BORDER_INNER); 
        gui.fill(r.x, r.bottom() - 1, r.right(), r.bottom(), COL_BORDER_INNER); 
        gui.fill(r.x, r.y, r.x + 1, r.bottom(), COL_BORDER_INNER); 
        gui.fill(r.right() - 1, r.y, r.right(), r.bottom(), COL_BORDER_INNER);
    }

    private void renderPreviewEntity(GuiGraphics gui, int mouseX, int mouseY) {
        // Calculate rendering area within the panel (below the header)
        int x = rPreview.x;
        int y = rPreview.y + PANEL_HEADER_HEIGHT;
        int w = rPreview.w;
        // Adjust height to stop BEFORE the buttons area (bottom ~90px)
        int buttonsHeight = 90;
        int h = rPreview.h - PANEL_HEADER_HEIGHT - buttonsHeight;

        // Ensure we have a valid drawing area
        int availableHeight = Math.max(h, 50);
        
        // Scale calculation
        int scale = Math.min((int)(availableHeight / 2.5), 80);
        float sensitivity = 0.25f;
        
        float centerX = x + w / 2.0f;
        float centerY = y + availableHeight / 2.0f + 20;
        
        float adjustedMouseX = centerX + (mouseX - centerX) * sensitivity;
        float adjustedMouseY = centerY + (mouseY - centerY) * sensitivity;

        if (dummyPlayer != null) {
            dummyPlayer.tickCount = (int)(Util.getMillis() / 50L);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                gui, 
                x + 5, y + 20, x + w - 5, y + availableHeight,
                scale, 0.0625f, 
                adjustedMouseX, adjustedMouseY, 
                dummyPlayer
            );
        } else {
            // Calculate true visual center for the text
            int textY = y + (availableHeight / 2) - (font.lineHeight / 2);
            gui.drawCenteredString(font, Component.translatable("bedrockskins.preview.unavailable"), (int)centerX, textY, COL_TEXT_SEC);
        }
    }

    // --- Utility Classes ---

    private static class Rect {
        int x, y, w, h;
        void set(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + (w / 2); }
        int centerY() { return y + (h / 2); }
    }
    
    /**
     * Optimizes reflection calls by caching accessors and preferring standard
     * Mojang mappings/Fabric accessors if available.
     */
    private static class ReflectionHelper {
        private static Method setX, setY, setLeftPos, setTopPos, setPosition;

        static void setPos(Object widget, int x, int y) {
            // 1. Fast path: Modern standard widgets (Fabric/Mojang mappings 1.19.4+)
            if (widget instanceof AbstractWidget aw) {
                aw.setX(x);
                aw.setY(y);
                return;
            }
            
            // 2. Slow path: Reflection fallback for obfuscated/custom widgets
            try {
                Class<?> clz = widget.getClass();
                
                // Try cached 'setPosition'
                if (setPosition != null) { setPosition.invoke(widget, x, y); return; }
                
                // Try finding standard setX/setY
                try {
                    Method mx = clz.getMethod("setX", int.class);
                    mx.invoke(widget, x);
                    try { clz.getMethod("setY", int.class).invoke(widget, y); } catch(Exception ignored){}
                    return;
                } catch (Exception ignored) {}

                // Try finding legacy setLeftPos
                try {
                    Method m = clz.getMethod("setLeftPos", int.class);
                    setLeftPos = m;
                    m.invoke(widget, x);
                    try { clz.getMethod("setTopPos", int.class).invoke(widget, y); } catch(Exception ignored){}
                    return;
                } catch (Exception ignored) {}
                
                // Try finding setPosition
                try {
                    Method m = clz.getMethod("setPosition", int.class, int.class);
                    setPosition = m;
                    m.invoke(widget, x, y);
                } catch (Exception ignored) {}

            } catch (Exception ignored) {
                // 3. Last Resort: Direct field access
                try {
                    var fx = widget.getClass().getField("x");
                    fx.setAccessible(true); fx.setInt(widget, x);
                    var fy = widget.getClass().getField("y");
                    fy.setAccessible(true); fy.setInt(widget, y);
                } catch (Exception e) { /* Give up */ }
            }
        }
    }
}