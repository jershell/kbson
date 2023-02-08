package com.github.jershell.kbson

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.bson.BsonType
import java.time.Instant
import java.time.temporal.TemporalAccessor

/**
 * This handles serialization and deserialization of arbitrary JSON trees represented as
 * `Map<String, Any?>`
 *
 * Using `@Serializable` objects in the tree respects their declared serializers.
 *
 */
object MapAnySerializer : KSerializer<Map<String, Any?>> {

    @Serializable
    private abstract class MapAnyMap : Map<String, Any?>

    override val descriptor: SerialDescriptor = MapAnyMap.serializer().descriptor

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        return when (decoder) {
            is JsonDecoder -> {
                val jsonObject = decoder.decodeJsonElement() as JsonObject
                jsonObject.toPrimitiveMap()
            }

            is BsonFlexibleDecoder -> {
                decoder.decodeStructure(descriptor) {
                    val map = mutableMapOf<String, Any?>()
                    parseBsonDocument(decoder, map)
                    decoder.reader.readEndDocument()
                    map.toMap()
                }
            }

            else -> {
                throw NotImplementedError("Decoder $decoder is not supported!")
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        when (encoder) {
            is JsonEncoder -> {
                encoder.encodeJsonElement(value.toJsonElement())
            }

            is BsonEncoder -> {
                encoder.encodeJson(Json.encodeToString(this, value))
            }

            else -> {
                throw NotImplementedError("Encoder $encoder is not supported!")
            }
        }
    }

    private fun parseBsonDocument(decoder: BsonFlexibleDecoder, map: MutableMap<String, Any?>) {
        var type = decoder.reader.readBsonType()
        while (type != BsonType.END_OF_DOCUMENT) {
            val key = decoder.reader.readName()
            when (type) {
                BsonType.NULL -> map[key] = decoder.decodeNull()
                BsonType.STRING -> map[key] = decoder.decodeString()
                BsonType.BOOLEAN -> map[key] = decoder.decodeBoolean()
                BsonType.INT32 -> map[key] = decoder.decodeInt()
                BsonType.INT64 -> map[key] = decoder.decodeLong()
                BsonType.DOUBLE -> map[key] = decoder.decodeDouble()
                BsonType.DECIMAL128 -> map[key] = decoder.reader.readDecimal128().toDouble()
                BsonType.DATE_TIME -> map[key] = Instant.ofEpochMilli(decoder.reader.readDateTime())
                BsonType.TIMESTAMP -> map[key] = Instant.ofEpochMilli(decoder.reader.readTimestamp().value)
                BsonType.BINARY -> map[key] = decoder.reader.readBinaryData()
                BsonType.DOCUMENT -> {
                    val innerMap = mutableMapOf<String, Any?>()
                    decoder.reader.readStartDocument()
                    parseBsonDocument(decoder, innerMap)
                    decoder.reader.readEndDocument()
                    map[key] = innerMap.toMap()
                }

                BsonType.ARRAY -> {
                    val innerList = mutableListOf<Any?>()
                    decoder.reader.readStartArray()
                    var arrayType = decoder.reader.readBsonType()
                    while (arrayType != BsonType.END_OF_DOCUMENT) {
                        when (arrayType) {
                            BsonType.NULL -> innerList.add(decoder.decodeNull())
                            BsonType.STRING -> innerList.add(decoder.decodeString())
                            BsonType.BOOLEAN -> innerList.add(decoder.decodeBoolean())
                            BsonType.INT32 -> innerList.add(decoder.decodeInt())
                            BsonType.INT64 -> innerList.add(decoder.decodeLong())
                            BsonType.DOUBLE -> innerList.add(decoder.decodeDouble())
                            BsonType.DECIMAL128 -> innerList.add(decoder.reader.readDecimal128().toDouble())
                            BsonType.DATE_TIME -> innerList.add(Instant.ofEpochMilli(decoder.reader.readDateTime()))
                            BsonType.TIMESTAMP -> innerList.add(Instant.ofEpochMilli(decoder.reader.readTimestamp().value))
                            BsonType.DOCUMENT -> {
                                val innerMap = mutableMapOf<String, Any?>()
                                decoder.reader.readStartDocument()
                                parseBsonDocument(decoder, innerMap)
                                decoder.reader.readEndDocument()
                                innerList.add(innerMap.toMap())
                            }

                            else -> throw IllegalStateException("Array BsonType '$arrayType' not supported!")
                        }
                        arrayType = decoder.reader.readBsonType()
                    }
                    decoder.reader.readEndArray()
                    map[key] = innerList.toList()
                }

                else -> throw IllegalStateException("BsonType '$type' not supported!")
            }
            type = decoder.reader.readBsonType()
        }
    }

    private fun JsonObject.toPrimitiveMap(): Map<String, Any?> =
        this.map {
            it.key to it.value.toPrimitive()
        }.toMap()

    private fun JsonElement.toPrimitive(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> this.toPrimitiveMap()
        is JsonArray -> this.map { it.toPrimitive() }
        is JsonPrimitive -> {
            if (isString) {
                contentOrNull
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull
            }
        }

        else -> null
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is TemporalAccessor -> JsonPrimitive(this.toString())
        is Enum<*> -> JsonPrimitive(this.toString())
        is Array<*> -> this.toJsonElement()
        is Map<*, *> -> this.toJsonElement()
        is Iterable<*> -> this.toJsonElement()
        else -> throw IllegalStateException("Can't serialize unknown type: $this")
    }

    private fun Iterable<*>.toJsonElement(): JsonElement {
        val list = mutableListOf<JsonElement>()
        this.forEach { value ->
            when (value) {
                null -> list.add(JsonNull)
                is JsonElement -> list.add(value)
                is Map<*, *> -> list.add(value.toJsonElement())
                is List<*> -> list.add(value.toJsonElement())
                is Array<*> -> list.add(value.toJsonElement())
                is Boolean -> list.add(JsonPrimitive(value))
                is Number -> list.add(JsonPrimitive(value))
                is TemporalAccessor -> list.add(JsonPrimitive(value.toString()))
                is String -> list.add(JsonPrimitive(value))
                is Enum<*> -> list.add(JsonPrimitive(value.toString()))
                else -> throw IllegalStateException("Can't serialize unknown collection type: $value")
            }
        }
        return JsonArray(list)
    }

    private fun Array<*>.toJsonElement(): JsonArray {
        val list = mutableListOf<JsonElement>()
        this.forEach { list.add(it.toJsonElement()) }
        return JsonArray(list)
    }

    private fun Map<*, *>.toJsonElement(): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        this.forEach { (key, value) ->
            key as String
            when (value) {
                null -> map[key] = JsonNull
                is JsonElement -> map[key] = value
                is Map<*, *> -> map[key] = value.toJsonElement()
                is Iterable<*> -> map[key] = value.toJsonElement()
                is Boolean -> map[key] = JsonPrimitive(value)
                is Number -> map[key] = JsonPrimitive(value)
                is TemporalAccessor -> map[key] = JsonPrimitive(value.toString())
                is String -> map[key] = JsonPrimitive(value)
                is Enum<*> -> map[key] = JsonPrimitive(value.toString())
                else -> throw IllegalStateException("Can't serialize unknown type: $value")
            }
        }
        return JsonObject(map)
    }
}
