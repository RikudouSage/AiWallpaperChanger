package cz.chrastecky.aiwallpaperchanger.dto

data class PremadePromptCustomParameter(
    val name: String,
    val expression: String,
    val conditions: List<PremadePromptCustomParameterCondition>,
    val description: String? = null,
)
