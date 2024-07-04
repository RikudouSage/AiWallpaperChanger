package cz.chrastecky.aiwallpaperchanger.dto

data class GenerateTextRequest(
    val prompt: String,
    val models: List<String>,
)
