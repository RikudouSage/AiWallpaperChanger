package cz.chrastecky.aiwallpaperchanger.dto

import android.graphics.Bitmap

data class EasyModePromptParameter(
    val name: String,
    val value: String?,
    val image: Bitmap?
)

data class EasyModePrompt(
    val name: String,
    val prompt: String,
    val featured: Boolean?,
    val targetPrompt: String?,
    val parameters: Map<String, List<EasyModePromptParameter>>,
    val image: Bitmap?
)
