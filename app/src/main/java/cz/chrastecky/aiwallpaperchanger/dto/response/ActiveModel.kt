package cz.chrastecky.aiwallpaperchanger.dto.response

data class ActiveModel(
    val name: String,
    val count: Int,
    val performance: Number,
    val queued: Int,
    val jobs: Int,
    val eta: Int,
    val type: ModelType,
)
