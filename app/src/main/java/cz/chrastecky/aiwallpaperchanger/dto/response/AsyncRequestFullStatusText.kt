package cz.chrastecky.aiwallpaperchanger.dto.response

data class AsyncRequestFullStatusText(
    val finished: Int,
    val done: Boolean,
    val faulted: Boolean,
    val isPossible: Boolean,
    val waitTime: Int,
    val generations: List<GenerationDetailText>,
)
