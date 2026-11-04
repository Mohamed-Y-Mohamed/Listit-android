package com.list_it.app.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Minimal in-memory Supabase (GoTrue auth + PostgREST) for tests.
 * Supports exactly the calls the app makes: signup, password sign-in, user, logout, recover,
 * select/insert/update/delete with `eq` filters, HEAD counts and the two RPCs.
 */
class FakeSupabase {
    data class Account(val id: String, val email: String, var password: String, var confirmed: Boolean, val name: String)

    val accounts = mutableListOf<Account>()
    val tables = mutableMapOf<String, MutableList<JsonObject>>(
        "users" to mutableListOf(), "list" to mutableListOf(), "collection" to mutableListOf(),
        "task" to mutableListOf(), "note" to mutableListOf(), "app_colors" to mutableListOf(),
    )
    val requests = mutableListOf<String>()
    val resetEmails = mutableListOf<String>()
    var currentToken: String? = null
    private var tokenOwner: Account? = null
    var failTable: String? = null

    val engine = MockEngine { req -> handle(req) }

    private val json = Json { ignoreUnknownKeys = true }

    private fun body(req: HttpRequestData): String = when (val c = req.body) {
        is OutgoingContent.ByteArrayContent -> c.bytes().decodeToString()
        else -> ""
    }

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String, extra: Map<String, String> = emptyMap()): HttpResponseData =
        respond(body, status, headersOf(*(mapOf(HttpHeaders.ContentType to "application/json") + extra).map { it.key to listOf(it.value) }.toTypedArray()))

    private fun userJson(a: Account) = buildJsonObject {
        put("id", a.id); put("aud", "authenticated"); put("email", a.email); put("role", "authenticated")
        put("created_at", "2026-01-02T10:00:00Z"); put("updated_at", "2026-01-02T10:00:00Z")
        put("user_metadata", buildJsonObject { put("full_name", a.name) })
        put("identities", JsonArray(listOf(buildJsonObject { put("id", a.id); put("user_id", a.id); put("provider", "email"); put("identity_id", a.id)
            put("identity_data", buildJsonObject { put("email", a.email); put("sub", a.id) })
            put("created_at", "2026-01-02T10:00:00Z"); put("updated_at", "2026-01-02T10:00:00Z"); put("last_sign_in_at", "2026-01-02T10:00:00Z") })))
        if (a.confirmed) put("email_confirmed_at", "2026-01-02T10:00:00Z")
    }

    private fun MockRequestHandleScope.authError(code: String, msg: String) =
        json(HttpStatusCode.BadRequest, """{"code":400,"error_code":"$code","msg":"$msg"}""")

    private fun MockRequestHandleScope.handle(req: HttpRequestData): HttpResponseData {
        val path = req.url.encodedPath
        requests += "${req.method.value} $path${if (req.url.encodedQuery.isNotEmpty()) "?" + req.url.encodedQuery else ""}"
        if (path.startsWith("/auth/v1/")) return auth(req, path.removePrefix("/auth/v1/"))
        if (path.startsWith("/rest/v1/rpc/")) return rpc(path.removePrefix("/rest/v1/rpc/"))
        if (path.startsWith("/rest/v1/")) return rest(req, path.removePrefix("/rest/v1/"))
        return json(HttpStatusCode.NotFound, "{}")
    }

    private fun MockRequestHandleScope.auth(req: HttpRequestData, op: String): HttpResponseData {
        val payload = body(req).takeIf { it.isNotBlank() }?.let { json.parseToJsonElement(it).jsonObject }
        return when (op) {
            "signup" -> {
                val email = payload!!["email"]!!.jsonPrimitive.content
                val existing = accounts.firstOrNull { it.email == email }
                val name = payload["data"]?.jsonObject?.get("full_name")?.jsonPrimitive?.content.orEmpty()
                val acc = existing ?: Account(UUID.randomUUID().toString(), email, payload["password"]!!.jsonPrimitive.content, false, name).also { accounts += it }
                json(HttpStatusCode.OK, userJson(acc).toString())
            }
            "token" -> {
                val email = payload!!["email"]!!.jsonPrimitive.content
                val acc = accounts.firstOrNull { it.email == email && it.password == payload["password"]!!.jsonPrimitive.content }
                    ?: return json(HttpStatusCode.BadRequest, """{"error":"invalid_grant","error_description":"Invalid login credentials"}""")
                if (!acc.confirmed) return authError("email_not_confirmed", "Email not confirmed")
                tokenOwner = acc
                currentToken = "token-${acc.id}"
                json(HttpStatusCode.OK, buildJsonObject {
                    put("access_token", currentToken); put("token_type", "bearer"); put("expires_in", 3600)
                    put("refresh_token", "refresh"); put("user", userJson(acc))
                }.toString())
            }
            "user" -> tokenOwner?.let { json(HttpStatusCode.OK, userJson(it).toString()) } ?: json(HttpStatusCode.Unauthorized, """{"msg":"no session"}""")
            "logout" -> { tokenOwner = null; currentToken = null; respond("", HttpStatusCode.NoContent) }
            "recover" -> { resetEmails += payload!!["email"]!!.jsonPrimitive.content; json(HttpStatusCode.OK, "{}") }
            else -> json(HttpStatusCode.NotFound, "{}")
        }
    }

    private fun MockRequestHandleScope.rpc(fn: String): HttpResponseData {
        when (fn) {
            "delete_user_by_id", "delete_user_account" -> {
                val owner = tokenOwner
                if (fn == "delete_user_account" && owner != null) {
                    accounts.remove(owner)
                    tables.values.forEach { rows -> rows.removeAll { it["user_id"]?.jsonPrimitive?.content == owner.id || it["id"]?.jsonPrimitive?.content == owner.id } }
                }
                if (fn == "delete_user_by_id") accounts.removeIf { !it.confirmed }
            }
        }
        return respond("", HttpStatusCode.NoContent)
    }

    private fun filters(req: HttpRequestData): Map<String, String> = req.url.parameters.entries()
        .filter { it.key !in setOf("select", "order", "columns") }
        .associate { it.key to it.value.first().removePrefix("eq.") }

    private fun matches(row: JsonObject, f: Map<String, String>) = f.all { (k, v) ->
        val value = row[k]
        value is JsonPrimitive && value !is JsonNull && value.content == v
    }

    private fun MockRequestHandleScope.rest(req: HttpRequestData, table: String): HttpResponseData {
        val rows = tables.getOrPut(table) { mutableListOf() }
        if (failTable == table) return json(HttpStatusCode.BadRequest, """{"message":"boom from $table","code":"XX000"}""")
        val f = filters(req)
        return when (req.method) {
            HttpMethod.Get, HttpMethod.Head -> {
                val result = rows.filter { matches(it, f) }
                val order = req.url.parameters["order"]
                val sorted = if (order != null) result.sortedBy { it[order.substringBefore(".")]?.jsonPrimitive?.content } else result
                val range = mapOf("Content-Range" to "0-${sorted.size - 1}/${sorted.size}")
                if (req.method == HttpMethod.Head) json(HttpStatusCode.OK, "", range)
                else json(HttpStatusCode.OK, JsonArray(sorted).toString(), range)
            }
            HttpMethod.Post -> {
                val parsed: JsonElement = json.parseToJsonElement(body(req))
                val items = if (parsed is JsonArray) parsed.map { it.jsonObject } else listOf(parsed.jsonObject)
                rows += items
                json(HttpStatusCode.Created, "[]")
            }
            HttpMethod.Patch -> {
                val patch = json.parseToJsonElement(body(req)).jsonObject
                rows.replaceAll { if (matches(it, f)) JsonObject(it + patch) else it }
                json(HttpStatusCode.OK, "[]")
            }
            HttpMethod.Delete -> {
                rows.removeAll { matches(it, f) }
                respond("", HttpStatusCode.NoContent)
            }
            else -> json(HttpStatusCode.MethodNotAllowed, "{}")
        }
    }

    /** Seeds a confirmed account with its `users` row and returns it. */
    fun seedUser(email: String = "abdul@test.com", password: String = "secret1", name: String = "Abdul Moiz"): Account {
        val acc = Account(UUID.randomUUID().toString(), email, password, true, name)
        accounts += acc
        tables["users"]!! += buildJsonObject { put("id", acc.id); put("full_name", name); put("email", email); put("created_at", "2026-01-02T10:00:00.123456+00:00") }
        return acc
    }
}
