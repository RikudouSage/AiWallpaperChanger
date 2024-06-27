package cz.chrastecky.aiwallpaperchanger.dto

import java.util.Date
import java.util.UUID

data class StoredRequest(
    val id: UUID,
    val request: GenerateRequest,
    val seed: String,
    val workerId: String,
    val workerName: String,
    val created: Date,
    val model: String?,
)
