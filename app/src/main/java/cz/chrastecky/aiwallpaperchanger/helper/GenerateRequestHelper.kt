package cz.chrastecky.aiwallpaperchanger.helper

import com.google.gson.Gson
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequestOld
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt

class GenerateRequestHelper {

    companion object {
        @JvmStatic
        fun parse (raw: String): GenerateRequest {
            var request = Gson().fromJson(raw, GenerateRequest::class.java)

            // this can happen due to the deserialization
            @Suppress("SENSELESS_COMPARISON")
            if (request.models == null) {
                val oldRequest = Gson().fromJson(raw, GenerateRequestOld::class.java)
                request = request.copy(models = listOf(oldRequest.model))
            }

            return request
        }

        @JvmStatic
        fun disableNsfw(request: GenerateRequest): GenerateRequest {
            return request.copy(nsfw = false, censorNsfw = true)
        }

        @JvmStatic
        fun withPrompt(request: GenerateRequest, prompt: String): GenerateRequest {
            return request.copy(prompt = prompt)
        }

        @JvmStatic
        fun withStyle(request: GenerateRequest, style: PremadePrompt): GenerateRequest {
            return request.copy(
                prompt = style.prompt,
                models = style.models,
                negativePrompt = style.negativePrompt,
                hiresFix = style.hiresFix ?: request.hiresFix,
            )
        }

        @JvmStatic
        fun makeExtraSlow(request: GenerateRequest, slow: Boolean = true): GenerateRequest {
            return request.copy(
                extraSlowWorkers = slow,
            )
        }
    }

}