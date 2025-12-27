package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.pack.LoadedSkin
import com.google.gson.Gson
import net.minecraft.client.MinecraftClient
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object FavoritesManager {
    private val favoriteKeys = mutableListOf<String>()

    fun load() {
        favoriteKeys.clear()
        try {
            val state = StateManager.readState()
            favoriteKeys.addAll(state.favorites)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            val selected = SkinManager.getLocalSelectedKey()
            StateManager.saveState(favoriteKeys.toList(), selected)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isFavorite(skin: LoadedSkin): Boolean {
        return favoriteKeys.contains(skin.key)
    }

    fun addFavorite(skin: LoadedSkin) {
        if (!isFavorite(skin)) {
            favoriteKeys.add(0, skin.key) // Add to top
            save()
        }
    }

    fun removeFavorite(skin: LoadedSkin) {
        if (favoriteKeys.remove(skin.key)) {
            save()
        }
    }
    
    fun getFavoriteKeys(): List<String> {
        return favoriteKeys.toList()
    }
}
