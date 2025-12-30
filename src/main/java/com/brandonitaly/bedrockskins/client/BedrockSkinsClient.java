package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.InputConstants;
import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

import java.io.File;

public class BedrockSkinsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerKeyBinding();
        registerLifecycleEvents();
        registerNetworking();
    }

    private void registerKeyBinding() {
        try {
            String keyId = "key.bedrockskins.open";
            //? if <=1.21.8 {
            /*KeyBinding key = new KeyBinding(keyId, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "bedrockskins.controls");*/
            //?} else {
            KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));
            KeyMapping key = new KeyMapping(keyId, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category);
            //?}
            KeyMapping openKey = KeyBindingHelper.registerKeyBinding(key);

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (openKey.consumeClick()) {
                    client.setScreen(new SkinSelectionScreen(client.screen));
                }
            });
            System.out.println("BedrockSkinsClient: Registered keybinding (K)");
        } catch (Exception e) {
            System.out.println("BedrockSkinsClient: Keybinding error: " + e);
        }
    }

    private void registerLifecycleEvents() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            reloadResources(client);

            // Register a synchronous reload listener that triggers a resource reload
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> applySavedSkinOnJoin(client));
    }

    private final class Reloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override
        public Identifier getFabricId() {
            return Identifier.fromNamespaceAndPath("bedrockskins", "reloader");
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            reloadResources(Minecraft.getInstance());
        }
    }

    private void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();

            if (client.player != null) {
                String localUuid = client.player.getUUID().toString();
                String key = SkinManager.getSkin(localUuid);
                if (key != null) {
                    String[] parts = key.split(":", 2);
                    String pack = parts.length == 2 ? parts[0] : "Remote";
                    String name = parts.length == 2 ? parts[1] : key;
                    SkinManager.setSkin(localUuid, pack, name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applySavedSkinOnJoin(Minecraft client) {
        try {
            BedrockSkinsState state = StateManager.readState();
            String savedKey = state.getSelected();
            if (savedKey == null) return;
            if (client.player == null) return;

            String[] parts = savedKey.split(":", 2);
            String pack = parts.length == 2 ? parts[0] : "Remote";
            String name = parts.length == 2 ? parts[1] : savedKey;
            SkinManager.setSkin(client.player.getUUID().toString(), pack, name);

            com.brandonitaly.bedrockskins.pack.LoadedSkin loadedSkin = SkinPackLoader.loadedSkins.get(savedKey);
            if (loadedSkin != null) {
                byte[] textureData = loadTextureData(client, loadedSkin);
                if (textureData.length > 0) {
                    ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(savedKey, loadedSkin.getGeometryData().toString(), textureData));
                    System.out.println("BedrockSkinsClient: Synced skin " + savedKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID, (payload, context) -> {
            BedrockSkinsNetworking.SkinUpdatePayload p = payload;
            context.client().execute(() -> {
                String key = p.getSkinKey();
                java.util.UUID uuid = p.getUuid();
                String geom = p.getGeometry();
                byte[] tex = p.getTextureData();

                if ("RESET".equals(key)) {
                    SkinManager.resetSkin(uuid.toString());
                } else {
                    SkinPackLoader.registerRemoteSkin(key, geom, tex);
                    String[] parts = key.split(":", 2);
                    String pack = parts.length == 2 ? parts[0] : "Remote";
                    String name = parts.length == 2 ? parts[1] : key;
                    SkinManager.setSkin(uuid.toString(), pack, name);
                }
            });
        });
    }

    // --- Helpers ---

    private byte[] loadTextureData(Minecraft client, com.brandonitaly.bedrockskins.pack.LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource) {
                var resOpt = client.getResourceManager().getResource(((AssetSource.Resource) src).getId());
                if (resOpt.isPresent()) {
                    return resOpt.get().open().readAllBytes();
                }
                return new byte[0];
            } else if (src instanceof AssetSource.File) {
                File f = new File(((AssetSource.File) src).getPath());
                return java.nio.file.Files.readAllBytes(f.toPath());
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
