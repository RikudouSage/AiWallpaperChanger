package cz.chrastecky.aiwallpaperchanger.dto.response

data class GenerationDetailImage(
    val workerId: String,
    val workerName: String,
    val model: String,
    val state: String,
    val img: String,
    val seed: String,
    val id: String,
    val censored: Boolean,
)
