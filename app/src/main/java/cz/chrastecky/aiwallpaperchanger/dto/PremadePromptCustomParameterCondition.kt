package cz.chrastecky.aiwallpaperchanger.dto

data class PremadePromptCustomParameterCondition(
    val type: PremadePromptCustomParameterConditionType,
    val expression: String,
    val value: String,
)
