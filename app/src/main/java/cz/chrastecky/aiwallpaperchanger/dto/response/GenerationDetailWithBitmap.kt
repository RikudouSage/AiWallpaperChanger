package cz.chrastecky.aiwallpaperchanger.dto.response

import android.graphics.Bitmap

data class GenerationDetailWithBitmap(
    val detail: GenerationDetailImage,
    val image: Bitmap,
)
