package com.brandonitaly.bedrockskins.client;

import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?}

public class CustomCapesIntegration {
    private static final boolean CUSTOM_CAPES_LOADED;
    private static final boolean CUSTOM_SKINS_LOADED;
    
    static {
        //? if fabric {
        CUSTOM_CAPES_LOADED = FabricLoader.getInstance().isModLoaded("ep-custom-capes");
        CUSTOM_SKINS_LOADED = FabricLoader.getInstance().isModLoaded("customskins");
        //? } else if neoforge {
        /*CUSTOM_CAPES_LOADED = net.neoforged.fml.ModList.get().isLoaded("ep-custom-capes");
        CUSTOM_SKINS_LOADED = net.neoforged.fml.ModList.get().isLoaded("customskins");*/
        //?}
    }
    
    public static boolean isCustomCapesLoaded() {
        return CUSTOM_CAPES_LOADED;
    }
    
    public static boolean isCustomSkinsLoaded() {
        return CUSTOM_SKINS_LOADED;
    }
    
    public static boolean hasCustomCape(String uuid) {
        if (!CUSTOM_CAPES_LOADED) return false;
        
        try {
            Class<?> capeManagerClass = Class.forName("snownee.capes.CapeManager");
            Object capeManager = capeManagerClass.getMethod("getInstance").invoke(null);
            return (Boolean) capeManagerClass.getMethod("hasCape", String.class).invoke(capeManager, uuid);
        } catch (Exception e) {
            return false;
        }
    }
    
    //? if >=1.21.11 {
    public static Identifier getCustomCapeTexture(String uuid) {
        if (!CUSTOM_CAPES_LOADED) return null;
        
        try {
            Class<?> capeManagerClass = Class.forName("snownee.capes.CapeManager");
            Object capeManager = capeManagerClass.getMethod("getInstance").invoke(null);
            Object cape = capeManagerClass.getMethod("getCape", String.class).invoke(capeManager, uuid);
            
            if (cape != null) {
                Class<?> capeClass = cape.getClass();
                Object texture = capeClass.getMethod("getTexture").invoke(cape);
                if (texture instanceof Identifier) {
                    return (Identifier) texture;
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
    //?} else {
    /*public static ResourceLocation getCustomCapeTexture(String uuid) {
        if (!CUSTOM_CAPES_LOADED) return null;
        
        try {
            Class<?> capeManagerClass = Class.forName("snownee.capes.CapeManager");
            Object capeManager = capeManagerClass.getMethod("getInstance").invoke(null);
            Object cape = capeManagerClass.getMethod("getCape", String.class).invoke(capeManager, uuid);
            
            if (cape != null) {
                Class<?> capeClass = cape.getClass();
                Object texture = capeClass.getMethod("getTexture").invoke(cape);
                if (texture instanceof ResourceLocation) {
                    return (ResourceLocation) texture;
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }*/
    //?}
    
    public static boolean shouldOverrideWithCustomCape() {
        return BedrockSkinsConfig.isAllowPackCapeOverride() && hasCustomCape(Minecraft.getInstance().player.getUUID().toString());
    }
}