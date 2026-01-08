package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class SkinSelectionScreen extends Screen {

    // --- Widgets & State ---
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent;
    
    private String selectedPackId;
    private boolean wasMousePressed = false;
    
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    // Layout
    private final Rect rPacks = new Rect();
    private final Rect rSkins = new Rect();
    private final Rect rPreview = new Rect();

    public SkinSelectionScreen(Screen parent) {
        super(Component.translatable("bedrockskins.gui.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        
        buildSkinCache();
        calculateLayout();
        initWidgets();
    }
    
    private void buildSkinCache() {
        skinCache.clear();
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            skinCache.computeIfAbsent(skin.getId(), k -> new ArrayList<>()).add(skin);
        }
        
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
        
        int sideW = Math.max(130, Math.min(200, (int)(fullW * 0.22)));
        int centerW = fullW - (sideW * 2) - (gap * 2);
        
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
        int PANEL_HEADER_HEIGHT = 24;
        int PANEL_PADDING = 4;

        // -- Pack List --
        int plY = rPacks.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int plH = rPacks.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);
        
        packList = new SkinPackListWidget(minecraft, rPacks.w - (PANEL_PADDING * 2), plH, plY, 24,
                this::selectPack,
                id -> Objects.equals(selectedPackId, id),
                font);
        ReflectionHelper.setPos(packList, rPacks.x + PANEL_PADDING, plY);
        addRenderableWidget(packList);

        // -- Preview Panel --
        // Pass "this::addRenderableWidget" to allow the panel to add buttons to this screen
        previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
        previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this::addRenderableWidget);

        // -- Skin Grid --
        int sgY = rSkins.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int sgH = rSkins.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);
        
        skinGrid = new SkinGridWidget(minecraft, rSkins.w - (PANEL_PADDING * 2), sgH, sgY, 90,
                skin -> previewPanel.setSelectedSkin(skin), 
                () -> previewPanel != null ? previewPanel.getSelectedSkin() : null,
                font,
                this::safeRegisterTexture,
                SkinManager::setPreviewSkin,
                this::safeResetPreview);
        ReflectionHelper.setPos(skinGrid, rSkins.x + PANEL_PADDING, sgY);
        addRenderableWidget(skinGrid);

        // -- Footer Buttons --
        initFooterButtons();
        
        refreshPackList();

        if (selectedPackId != null) {
            selectPack(selectedPackId);
        }
    }
    
    private void onFavoritesChanged() {
        buildSkinCache();
        refreshPackList();
        if ("skinpack.Favorites".equals(selectedPackId)) {
            selectPack("skinpack.Favorites");
        }
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

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

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
        int itemWidth = 65;
        int totalWidth = rSkins.w - (4 * 2) - 10;
        int cols = Math.max(1, totalWidth / itemWidth);
        
        for (int i = 0; i < skins.size(); i += cols) {
            List<LoadedSkin> row = skins.subList(i, Math.min(i + cols, skins.size()));
            skinGrid.addSkinsRow(row);
        }
    }

    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, "skin_packs");
        if (!dir.exists()) dir.mkdirs();
        Util.getPlatform().openFile(dir);
    }

    private void safeRegisterTexture(String key) { try { SkinPackLoader.registerTextureFor(key); } catch (Exception ignored) {} }
    private void safeResetPreview(String uuid) { try { SkinManager.resetPreviewSkin(uuid); } catch (Exception ignored) {} }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        gui.drawCenteredString(font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        drawPanel(gui, rPacks, Component.translatable("bedrockskins.gui.packs"));
        drawPanel(gui, rSkins, Component.translatable("bedrockskins.gui.skins"));
        
        // Handle mouse events for preview panel
        boolean mousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
            org.lwjgl.glfw.GLFW.glfwGetCurrentContext(),
            org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        
        if (previewPanel != null) {
            // Detect mouse click
            if (mousePressed && !wasMousePressed) {
                previewPanel.mouseClicked(mouseX, mouseY, 0);
            }
            // Detect mouse release
            if (!mousePressed && wasMousePressed) {
                previewPanel.mouseReleased(mouseX, mouseY, 0);
            }
            wasMousePressed = mousePressed;
            
            previewPanel.render(gui, mouseX, mouseY);
        }
        
        super.render(gui, mouseX, mouseY, delta);
        
        // Render the full heart sprite AFTER buttons have rendered
        if (previewPanel != null) {
            previewPanel.renderSprites(gui);
        }
    }

    private void drawPanel(GuiGraphics gui, Rect r, Component title) {
        int PANEL_HEADER_HEIGHT = 24;
        int COL_PANEL_BG = 0xE6181818;
        int COL_PANEL_HEADER = 0xFF252525;
        int COL_BORDER_OUTER = 0xFF000000;
        int COL_BORDER_INNER = 0xFF383838;
        int COL_TEXT_TITLE = 0xFFFFFFFF;

        gui.fill(r.x - 1, r.y - 1, r.right() + 1, r.bottom() + 1, COL_BORDER_OUTER);
        gui.fill(r.x, r.y, r.right(), r.bottom(), COL_PANEL_BG);
        gui.fill(r.x, r.y, r.right(), r.y + PANEL_HEADER_HEIGHT, COL_PANEL_HEADER);
        gui.fill(r.x, r.y + PANEL_HEADER_HEIGHT, r.right(), r.y + PANEL_HEADER_HEIGHT + 1, COL_BORDER_INNER);
        gui.drawCenteredString(font, title, r.centerX(), r.y + 8, COL_TEXT_TITLE);
        
        gui.fill(r.x, r.y, r.right(), r.y + 1, COL_BORDER_INNER); 
        gui.fill(r.x, r.bottom() - 1, r.right(), r.bottom(), COL_BORDER_INNER); 
        gui.fill(r.x, r.y, r.x + 1, r.bottom(), COL_BORDER_INNER); 
        gui.fill(r.right() - 1, r.y, r.right(), r.bottom(), COL_BORDER_INNER);
    }

    private static class Rect {
        int x, y, w, h;
        void set(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + (w / 2); }
    }
    
    private static class ReflectionHelper {
        private static Method setPosition;

        static void setPos(Object widget, int x, int y) {
            if (widget instanceof AbstractWidget aw) {
                aw.setX(x);
                aw.setY(y);
                return;
            }
            try {
                Class<?> clz = widget.getClass();
                if (setPosition != null) { setPosition.invoke(widget, x, y); return; }
                
                try {
                    Method mx = clz.getMethod("setX", int.class);
                    mx.invoke(widget, x);
                    try { clz.getMethod("setY", int.class).invoke(widget, y); } catch(Exception ignored){}
                    return;
                } catch (Exception ignored) {}

                try {
                    Method m = clz.getMethod("setLeftPos", int.class);
                    m.invoke(widget, x);
                    try { clz.getMethod("setTopPos", int.class).invoke(widget, y); } catch(Exception ignored){}
                    return;
                } catch (Exception ignored) {}
                
                try {
                    Method m = clz.getMethod("setPosition", int.class, int.class);
                    setPosition = m;
                    m.invoke(widget, x, y);
                } catch (Exception ignored) {}

            } catch (Exception ignored) {
                try {
                    var fx = widget.getClass().getField("x");
                    fx.setAccessible(true); fx.setInt(widget, x);
                    var fy = widget.getClass().getField("y");
                    fy.setAccessible(true); fy.setInt(widget, y);
                } catch (Exception e) { }
            }
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}