package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    private static final boolean LEGACY4J_LOADED = FabricLoader.getInstance().isModLoaded("legacy");
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (LEGACY4J_LOADED) {
            return parent -> {
                try {
                    // Use reflection to avoid loading Legacy4J classes when the mod isn't present
                    Class<?> screenClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen");
                    var constructor = screenClass.getConstructor(Screen.class);
                    return (Screen) constructor.newInstance(parent);
                } catch (Exception e) {
                    // Fallback to standard screen if Legacy4J integration fails
                    return new SkinSelectionScreen((Screen) parent);
                }
            };
        }
        return parent -> new SkinSelectionScreen((Screen) parent);
    }
}
