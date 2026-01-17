package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import java.util.*;

/**
 * Adapter to organize LoadedSkins into pack-based collections
 * compatible with the Legacy4J carousel system.
 */
public class SkinPackAdapter {
    private final String packId;
    private final List<LoadedSkin> skins;
    private final String packType;

    public SkinPackAdapter(String packId, List<LoadedSkin> skins, String packType) {
        this.packId = packId;
        this.skins = new ArrayList<>(skins);
        this.packType = packType;
    }

    public String getPackType() {
        return packType;
    }

    // Legacy constructor for compatibility
    public SkinPackAdapter(String packId, List<LoadedSkin> skins) {
        this(packId, skins, null);
    }
    
    public String getPackId() {
        return packId;
    }
    
    public List<LoadedSkin> getSkins() {
        return skins;
    }
    
    public int size() {
        return skins.size();
    }
    
    public LoadedSkin getSkin(int ordinal) {
        if (ordinal >= 0 && ordinal < skins.size()) {
            return skins.get(ordinal);
        }
        return null;
    }
    
    public int indexOf(LoadedSkin skin) {
        return skins.indexOf(skin);
    }
    
    public int indexOf(SkinId skinId) {
        if (skinId == null) return -1;
        for (int i = 0; i < skins.size(); i++) {
            var id = skins.get(i).getSkinId();
            if (java.util.Objects.equals(id, skinId)) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean isEmpty() {
        return skins.isEmpty();
    }
    
    /**
     * Gets all available skin packs from the SkinPackLoader.
     */
    public static Map<String, SkinPackAdapter> getAllPacks() {
        Map<String, SkinPackAdapter> packs = new LinkedHashMap<>();
        Map<String, List<LoadedSkin>> packMap = new HashMap<>();

        // Group skins by pack ID
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            String packId = skin.getId();
            packMap.computeIfAbsent(packId, k -> new ArrayList<>()).add(skin);
        }

        // Create adapters with packType if available
        for (Map.Entry<String, List<LoadedSkin>> entry : packMap.entrySet()) {
            String packType = SkinPackLoader.packTypesByPackId.get(entry.getKey());
            packs.put(entry.getKey(), new SkinPackAdapter(entry.getKey(), entry.getValue(), packType));
        }

        return packs;
    }
    
    /**
     * Gets a specific pack by ID.
     */
    public static SkinPackAdapter getPack(String packId) {
        List<LoadedSkin> skins = new ArrayList<>();
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            if (skin.getId().equals(packId)) {
                skins.add(skin);
            }
        }
        String packType = SkinPackLoader.packTypesByPackId.get(packId);
        return new SkinPackAdapter(packId, skins, packType);
    }
}
