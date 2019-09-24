package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.Encoder
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.modules.serializersModuleOf
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.util.Date

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class NonEncodeNull


@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("DateSerializer")

    override fun serialize(encoder: Encoder, obj: Date) {
        encoder as BsonEncoder
        encoder.encodeDateTime(obj.time)
    }

    override fun deserialize(decoder: Decoder): Date {
        return when(decoder) {
            is BsonDocumentDecoder -> {
                Date(decoder.decodeTaggedDateTime())
            }
            is BsonDecoder -> {
                Date(decoder.reader.readDateTime())
            }
            else -> throw SerializationException("Unknown decoder type")
        }
    }
}


@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("DateSerializer")

    override fun serialize(encoder: Encoder, obj: BigDecimal) {
        encoder as BsonEncoder
        encoder.encodeDecimal128(Decimal128(obj))
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return when(decoder) {
            is BsonDocumentDecoder -> {
                decoder.decodeTaggedDecimal128().bigDecimalValue()
            }
            is BsonDecoder -> {
                decoder.reader.readDecimal128().bigDecimalValue()
            }
            else -> throw SerializationException("Unknown decoder type")
        }
    }
}


@Serializer(forClass = ByteArray::class)
object ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("DateSerializer")

    override fun serialize(encoder: Encoder, obj: ByteArray) {
        encoder as BsonEncoder
        encoder.encodeByteArray(obj)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return when(decoder) {
            is BsonDocumentDecoder -> {
                decoder.decodeByteArray()
            }
            is BsonDecoder -> {
                decoder.reader.readBinaryData().data
            }
            else -> throw SerializationException("Unknown decoder type")
        }

    }
}


@Serializer(forClass = ObjectId::class)
object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("ObjectIdSerializer")

    override fun serialize(encoder: Encoder, obj: ObjectId) {
        encoder as BsonEncoder
        encoder.encodeObjectId(obj)
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return when(decoder) {
            is BsonDocumentDecoder -> {
                decoder.decodeObjectId()
            }
            is BsonDecoder -> {
                decoder.reader.readObjectId()
            }
            else -> throw SerializationException("Unknown decoder type")
        }

    }
}

val DefaultModule = serializersModuleOf(mapOf(
        ObjectId::class to ObjectIdSerializer,
        BigDecimal::class to BigDecimalSerializer,
        ByteArray::class to ByteArraySerializer,
        Date::class to DateSerializer
))
