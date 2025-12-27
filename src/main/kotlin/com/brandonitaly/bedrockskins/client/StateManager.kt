package com.brandonitaly.bedrockskins.client

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.minecraft.client.MinecraftClient
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class BedrockSkinsState(
    @SerializedName("favorites") val favorites: List<String> = emptyList(),
    @SerializedName("selected") val selected: String? = null
)

object StateManager {
    private val gson = Gson()
    private val stateFile: File = File(MinecraftClient.getInstance().runDirectory, "bedrock_skins_state.json")

    fun readState(): BedrockSkinsState {
        try {
            if (!stateFile.exists()) return BedrockSkinsState()
            FileReader(stateFile).use { reader ->
                return gson.fromJson(reader, BedrockSkinsState::class.java) ?: BedrockSkinsState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return BedrockSkinsState()
        }
    }

    fun saveState(favorites: List<String>, selected: String?) {
        try {
            val tmp = File(stateFile.absolutePath + ".tmp")
            FileWriter(tmp).use { writer ->
                gson.toJson(BedrockSkinsState(favorites, selected), writer)
            }
            if (tmp.exists()) {
                if (stateFile.exists()) stateFile.delete()
                tmp.renameTo(stateFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
