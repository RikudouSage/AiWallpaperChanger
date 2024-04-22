package cz.chrastecky.aiwallpaperchanger.dto.response

import android.graphics.Bitmap

data class GenerationDetailWithBitmap(
    val detail: GenerationDetail,
    val image: Bitmap,
)
