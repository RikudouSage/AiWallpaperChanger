package cz.chrastecky.aiwallpaperchanger.dto

data class PremadePrompt(
    val name: String,
    val prompt: String,
    val models: List<String>,
    val negativePrompt: String? = null,
    val hiresFix: Boolean? = null,
    val params: Map<String, List<String>>? = null,
    val description: String? = null,
    val author: String? = null,
)
