package com.list_it.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AppUser(
    val id: String,
    @SerialName("created_at") @Serializable(IsoDateSerializer::class) val createdAt: Instant? = null,
    @SerialName("full_name") val name: String,
    val email: String,
)

/** A top-level List (named `List` in the iOS app). */
@Serializable
data class TodoList(
    val id: String,
    @SerialName("created_at") @Serializable(SupabaseDateSerializer::class) val createdAt: Instant,
    @SerialName("list_icon") val listIcon: String,
    @SerialName("list_name") val listName: String,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("bg_color_hex") val bgColorHex: String,
    @SerialName("user_id") val userId: String,
    @SerialName("is_pinned") val isPinned: Boolean,
)

/** A Collection inside a List (named `Collection` in the iOS app). */
@Serializable
data class ListCollection(
    val id: String,
    @SerialName("created_at") @Serializable(SupabaseDateSerializer::class) val createdAt: Instant,
    @SerialName("collection_name") val collectionName: String,
    @SerialName("bg_color_hex") val bgColorHex: String,
    @SerialName("list_id") val listID: String,
    @SerialName("user_id") val userID: String,
)

@Serializable
data class ToDoTask(
    val id: String,
    @SerialName("created_at") @Serializable(IsoDateSerializer::class) val createdAt: Instant,
    val text: String,
    val description: String? = null,
    @SerialName("due_date") @Serializable(IsoDateSerializer::class) val dueDate: Instant? = null,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("date_completed") @Serializable(IsoDateSerializer::class) val dateCompleted: Instant? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("is_pinned") val isPinned: Boolean,
    @SerialName("user_id") val userID: String,
    @SerialName("collection_id") val collectionID: String,
    @SerialName("list_id") val listID: String,
)

@Serializable
data class Note(
    val id: String,
    @SerialName("created_at") @Serializable(SupabaseDateSerializer::class) val createdAt: Instant,
    val title: String,
    val description: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("bg_color_hex") val bgColorHex: String,
    @SerialName("is_pinned") val isPinned: Boolean,
    @SerialName("collection_id") val collectionID: String,
    @SerialName("user_id") val userID: String,
    @SerialName("list_id") val listID: String,
)

@Serializable
data class AppColor(
    val id: Int,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("color_name") val colorName: String,
)

data class WhatsNewFeature(val icon: String, val title: String, val description: String)
