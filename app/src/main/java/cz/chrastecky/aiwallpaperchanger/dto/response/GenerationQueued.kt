package cz.chrastecky.aiwallpaperchanger.dto.response

data class GenerationQueued(
    val id: String,
    val kudos: Int,
    val message: String?,
    val warnings: List<HordeWarning>?,
)
