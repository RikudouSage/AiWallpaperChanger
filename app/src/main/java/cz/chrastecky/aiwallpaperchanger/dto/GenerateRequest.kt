package cz.chrastecky.aiwallpaperchanger.dto

data class GenerateRequest(
    val prompt: String,
    val negativePrompt: String?,
    val models: List<String>,
    val sampler: Sampler,
    val steps: Int,
    val clipSkip: Int,
    val width: Int,
    val height: Int,
    val faceFixer: FaceFixer?,
    val upscaler: String?,
    val cfgScale: Double,
    val nsfw: Boolean,
    val karras: Boolean,
    val hiresFix: Boolean,
    val extraSlowWorkers: Boolean,
    val censorNsfw: Boolean = false,
)
