package cz.chrastecky.aiwallpaperchanger.helper

import com.google.gson.Gson
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequestOld

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
            return request.copy(nsfw = false);
        }

        @JvmStatic
        fun withPrompt(request: GenerateRequest, prompt: String): GenerateRequest {
            return request.copy(prompt = prompt)
        }
    }

}