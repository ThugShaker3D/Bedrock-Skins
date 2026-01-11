package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import java.util.*;

/**
 * Adapter to organize LoadedSkins into pack-based collections
 * compatible with the Legacy4J carousel system.
 */
public class SkinPackAdapter {
    private final String packId;
    private final List<LoadedSkin> skins;
    
    public SkinPackAdapter(String packId, List<LoadedSkin> skins) {
        this.packId = packId;
        this.skins = new ArrayList<>(skins);
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
    
    public int indexOf(String skinKey) {
        for (int i = 0; i < skins.size(); i++) {
            if (skins.get(i).getKey().equals(skinKey)) {
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
        
        // Create adapters
        for (Map.Entry<String, List<LoadedSkin>> entry : packMap.entrySet()) {
            packs.put(entry.getKey(), new SkinPackAdapter(entry.getKey(), entry.getValue()));
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
        return new SkinPackAdapter(packId, skins);
    }
}
