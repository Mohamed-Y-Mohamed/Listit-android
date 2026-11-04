package com.list_it.app.data

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

/**
 * Gemini Nano through ML Kit's on-device Prompt API: the Android counterpart of Apple's
 * on-device Foundation Models used by the iOS app. Nothing leaves the device.
 */
object OnDeviceModel {
    private val model by lazy { Generation.getClient() }

    /** True when the device supports on-device generation (iOS `SystemLanguageModel.availability`). */
    suspend fun isAvailable(): Boolean =
        runCatching { model.checkStatus() != FeatureStatus.UNAVAILABLE }.getOrDefault(false)

    suspend fun respond(prompt: String): String =
        model.generateContent(prompt).candidates.firstOrNull()?.text.orEmpty()
}
