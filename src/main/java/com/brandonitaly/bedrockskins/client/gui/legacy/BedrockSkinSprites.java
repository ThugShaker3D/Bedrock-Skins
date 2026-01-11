package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

/**
 * Sprite constants for Legacy4J-styled UI elements.
 */
public class BedrockSkinSprites {
    //? if >=1.21.11 {
    public static final Identifier SKIN_BOX = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_box");
    public static final Identifier SKIN_PANEL = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_panel");
    public static final Identifier PANEL_FILLER = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/panel_filler");
    public static final Identifier PACK_NAME_BOX = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/pack_name_box");
    //?} else {
    /*public static final ResourceLocation SKIN_BOX = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/skin_box");
    public static final ResourceLocation SKIN_PANEL = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/skin_panel");
    public static final ResourceLocation PANEL_FILLER = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/panel_filler");
    public static final ResourceLocation PACK_NAME_BOX = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/pack_name_box");*/
    //?}
}
