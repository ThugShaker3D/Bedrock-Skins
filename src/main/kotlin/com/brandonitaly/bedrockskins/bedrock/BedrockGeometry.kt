package com.brandonitaly.bedrockskins.bedrock

import com.google.gson.annotations.SerializedName

data class BedrockFile(
    @SerializedName("format_version") val formatVersion: String,
    @SerializedName("minecraft:geometry") val geometries: List<BedrockGeometry>?
)

data class BedrockGeometry(
    val description: GeometryDescription,
    var bones: List<BedrockBone>?,
    @SerializedName("animationArmsOutFront") val animationArmsOutFront: Boolean? = false,
    @SerializedName("animationStationaryLegs") val animationStationaryLegs: Boolean? = false
)

data class GeometryDescription(
    val identifier: String,
    @SerializedName("texture_width") val textureWidth: Int,
    @SerializedName("texture_height") val textureHeight: Int,
    @SerializedName("visible_bounds_width") val visibleBoundsWidth: Float?,
    @SerializedName("visible_bounds_height") val visibleBoundsHeight: Float?,
    @SerializedName("visible_bounds_offset") val visibleBoundsOffset: List<Float>?
)

data class BedrockBone(
    val name: String,
    val parent: String?,
    val pivot: List<Float>?,
    val rotation: List<Float>?,
    val cubes: List<BedrockCube>?,
    val locators: Map<String, List<Float>>?,
    val inflate: Float?,
    val mirror: Boolean?
)

data class BedrockCube(
    val origin: List<Float>,
    val size: List<Float>,
    val uv: Any?, // Can be [u, v] or { "uv": [u, v], "uv_size": [w, h] } (Box UV vs Per-Face UV)
    val inflate: Float?,
    val mirror: Boolean?,
    val rotation: List<Float>? // Cube rotation
)
