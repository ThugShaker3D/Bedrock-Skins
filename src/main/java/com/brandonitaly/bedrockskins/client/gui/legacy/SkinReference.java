package com.brandonitaly.bedrockskins.client.gui.legacy;

/**
 * Adapter class to bridge LoadedSkin structure with Legacy4J widget system.
 * Represents a reference to a specific skin within a pack.
 */
public record SkinReference(String packId, int ordinal) {
    public String toKey() {
        return packId + ":" + ordinal;
    }
    
    public static SkinReference fromKey(String key) {
        String[] parts = key.split(":", 2);
        if (parts.length == 2) {
            try {
                return new SkinReference(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
