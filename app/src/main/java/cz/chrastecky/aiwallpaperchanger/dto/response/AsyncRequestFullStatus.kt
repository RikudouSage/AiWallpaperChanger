package cz.chrastecky.aiwallpaperchanger.dto.response

data class AsyncRequestFullStatus(
    val finished: Int,
    val processing: Int,
    val restarted: Int,
    val waiting: Int,
    val done: Boolean,
    val faulted: Boolean,
    val waitTime: Int,
    val queuePosition: Int,
    val kudos: Int,
    val isPossible: Boolean,
    val generations: List<GenerationDetail>,
    val shared: Boolean,
)
