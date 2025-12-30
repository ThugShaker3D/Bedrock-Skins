package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

public class PreviewPlayer extends RemotePlayer {

    public PreviewPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    // --- VISUAL FIX ---
    // This forces the game to render the outer skin layers (Hat, Jacket, Pants, Sleeves)
    // without needing to hack the DataTracker with reflection.
    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
    }

    // --- POOL ---
    public static final class PreviewPlayerPool {
        private static final Map<UUID, PreviewPlayer> pool = new ConcurrentHashMap<>();

        public static PreviewPlayer get(ClientLevel world, GameProfile profile) {
            UUID id;
            
            try {
                // Try standard getter via reflection
                java.lang.reflect.Method m = GameProfile.class.getMethod("getId");
                id = (UUID) m.invoke(profile);
            } catch (Exception e) {
                // Fallback to field access if getter fails
                try {
                    java.lang.reflect.Field f = GameProfile.class.getDeclaredField("id");
                    f.setAccessible(true);
                    id = (UUID) f.get(profile);
                } catch (Exception ex) {
                    // Ultimate fallback
                    id = UUID.randomUUID();
                }
            }

            return pool.computeIfAbsent(id, k -> new PreviewPlayer(world, profile));
        }

        public static void remove(UUID id) { pool.remove(id); }
        public static void clear() { pool.clear(); }
    }
}