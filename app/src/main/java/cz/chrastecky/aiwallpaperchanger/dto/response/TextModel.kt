package cz.chrastecky.aiwallpaperchanger.dto.response

data class TextModel(
    val performance: Double,
    val queued: Int,
    val jobs: Int,
    val eta: Int,
    val type: String,
    val name: String,
    val count: Int,
)
