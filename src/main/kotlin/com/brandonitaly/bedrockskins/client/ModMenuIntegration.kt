package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> SkinSelectionScreen(parent) }
    }
}
