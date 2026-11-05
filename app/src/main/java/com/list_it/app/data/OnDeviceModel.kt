package com.list_it.app.data

import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

/**
 * Gemini Nano through ML Kit's on-device Prompt API: the Android counterpart of Apple's
 * on-device Foundation Models used by the iOS app. Nothing leaves the device.
 */
object OnDeviceModel {
    /**
     * ML Kit GenAI requires API 26; the app supports API 24. Nothing here may touch [Generation]
     * below that, or loading the class throws an Error that the AI error handling cannot catch.
     */
    private val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private const val TAG = "OnDeviceModel"

    private val model by lazy { Generation.getClient() }

    /** True when the device supports on-device generation (iOS `SystemLanguageModel.availability`). */
    suspend fun isAvailable(): Boolean {
        if (!supported) return false
        return runCatching { model.checkStatus() }
            .onSuccess { Log.i(TAG, "Gemini Nano status: $it (0=unavailable 1=downloadable 2=downloading 3=available)") }
            .onFailure { Log.w(TAG, "Gemini Nano status check failed; AI features hidden", it) }
            .map { it != FeatureStatus.UNAVAILABLE }
            .getOrDefault(false)
    }

    suspend fun respond(prompt: String): String {
        check(supported) { "On-device AI needs Android 8.0 or newer." }
        return model.generateContent(prompt).candidates.firstOrNull()?.text.orEmpty()
    }
}
