package com.list_it.app.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/** Parses every date format the iOS app reads from Supabase. Strings without an offset are UTC. */
object SupabaseDates {
    private val local: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .optionalStart().appendLiteral('T').optionalEnd()
        .optionalStart().appendLiteral(' ').optionalEnd()
        .appendPattern("HH:mm:ss")
        .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
        .toFormatter()

    private val plain: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

    fun parse(value: String): Instant =
        runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { OffsetDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS][.SSS]XXX")).toInstant() }
            .recoverCatching { LocalDateTime.parse(value, local).toInstant(ZoneOffset.UTC) }
            .getOrElse { throw IllegalArgumentException("Invalid date: $value") }

    /** "yyyy-MM-dd HH:mm:ss", the format the iOS app writes for lists, collections and notes. */
    fun formatPlain(instant: Instant): String = plain.format(instant)

    /** ISO 8601 with fractional seconds, the format the iOS app writes for tasks. */
    fun formatIso(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
}

object SupabaseDateSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("SupabaseDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(SupabaseDates.formatPlain(value))
    override fun deserialize(decoder: Decoder): Instant = SupabaseDates.parse(decoder.decodeString())
}

object IsoDateSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("IsoDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(SupabaseDates.formatIso(value))
    override fun deserialize(decoder: Decoder): Instant = SupabaseDates.parse(decoder.decodeString())
}
