package com.list_it.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/** Supabase client with Auth + Postgrest. [engine] is only overridden by tests. */
fun createListItClient(url: String, key: String, engine: HttpClientEngine? = null): SupabaseClient =
    createSupabaseClient(url, key) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        if (engine != null) httpEngine = engine
        install(Auth) {
            // Tests run without an Android Context, so keep their session in memory.
            if (engine != null) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                enableLifecycleCallbacks = false
            }
        }
        install(Postgrest)
    }
