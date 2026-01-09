package com.brandonitaly.bedrockskins.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class SkinManager {
    private SkinManager() {}

    private static final Map<String, String> playerSkins = new HashMap<>();

    public static void load() {
        playerSkins.clear();
        try {
            var state = StateManager.readState();
            var selected = state.getSelected();
            var client = Minecraft.getInstance();
            var player = client.player;
            if (selected != null && !selected.isEmpty() && player != null) {
                playerSkins.put(player.getUUID().toString(), selected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalSelectedKey() {
        var client = Minecraft.getInstance();
        var localUuid = client.player != null ? client.player.getUUID().toString() : null;
        if (localUuid == null) return null;
        return playerSkins.get(localUuid);
    }

    public static void setSkin(String uuid, String packName, String skinName) {
        String key = packName + ":" + skinName;
        playerSkins.put(uuid, key);
        var client = Minecraft.getInstance();
        var localUuid = client.player != null ? client.player.getUUID().toString() : null;
        if (localUuid != null && localUuid.equals(uuid)) {
            try {
                var favorites = FavoritesManager.getFavoriteKeys();
                StateManager.saveState(favorites, key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPreviewSkin(String uuid, String packName, String skinName) {
        String key = packName + ":" + skinName;
        playerSkins.put(uuid, key);
    }

    public static void resetPreviewSkin(String uuid) {
        playerSkins.remove(uuid);
    }

    public static String getSkin(String uuid) {
        return playerSkins.get(uuid);
    }

    public static void resetSkin(String uuid) {
        if (playerSkins.remove(uuid) != null) {
            var client = Minecraft.getInstance();
            var localUuid = client.player != null ? client.player.getUUID().toString() : null;
            if (localUuid != null && localUuid.equals(uuid)) {
                try {
                    var favorites = FavoritesManager.getFavoriteKeys();
                    StateManager.saveState(favorites, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
