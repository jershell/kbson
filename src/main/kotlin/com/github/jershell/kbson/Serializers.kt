package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.modules.serializersModuleOf
import org.bson.BsonType
import org.bson.UuidRepresentation
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.util.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class NonEncodeNull


@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("DateSerializer",PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: Date) {
        encoder as BsonEncoder
        encoder.encodeDateTime(obj.time)
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): Date {
        return when (decoder) {
            is FlexibleDecoder -> {
                Date(
                    when (decoder.reader.currentBsonType) {
                        BsonType.STRING -> decoder.decodeString().toLong()
                        BsonType.DATE_TIME -> decoder.reader.readDateTime()
                        else -> throw SerializationException("Unsupported ${decoder.reader.currentBsonType} reading date")
                    }
                )
            }
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
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("BigDecimalSerializer",PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: BigDecimal) {
        encoder as BsonEncoder
        encoder.encodeDecimal128(Decimal128(obj))
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): BigDecimal {
        return when (decoder) {
            is FlexibleDecoder -> {
                when (decoder.reader.currentBsonType) {
                    BsonType.STRING -> BigDecimal(decoder.decodeString())
                    BsonType.DECIMAL128 -> decoder.reader.readDecimal128().bigDecimalValue()
                    else -> throw SerializationException("Unsupported ${decoder.reader.currentBsonType} reading decimal128")
                }
            }
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
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("ByteArraySerializer",PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: ByteArray) {
        encoder as BsonEncoder
        encoder.encodeByteArray(obj)
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): ByteArray {
        return when (decoder) {
            is FlexibleDecoder -> {
                decoder.reader.readBinaryData().data
            }
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
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("ObjectIdSerializer",PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: ObjectId) {
        encoder as BsonEncoder
        encoder.encodeObjectId(obj)
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): ObjectId {
        return when (decoder) {
            is FlexibleDecoder -> {
                when (decoder.reader.currentBsonType) {
                    BsonType.STRING -> ObjectId(decoder.decodeString())
                    BsonType.OBJECT_ID -> decoder.reader.readObjectId()
                    else -> throw SerializationException("Unsupported ${decoder.reader.currentBsonType} reading object id")
                }
            }
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

@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("UUIDSerializer",PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: UUID) {
        encoder as BsonEncoder
        encoder.encodeUUID(obj, UuidRepresentation.STANDARD)
    }

    override fun deserialize(decoder: Decoder): UUID {
        return when (decoder) {
            is FlexibleDecoder -> {
                when (decoder.reader.currentBsonType) {
                    BsonType.STRING -> {
                       UUID.fromString(decoder.decodeString())
                    }
                    BsonType.BINARY -> {
                        decoder.reader.readBinaryData().asUuid(UuidRepresentation.STANDARD)
                    }
                    else -> throw SerializationException("Unsupported ${decoder.reader.currentBsonType} reading object id")
                }
            }
            else -> throw SerializationException("Unknown decoder type")
        }
    }
}

val DefaultModule = serializersModuleOf(
    mapOf(
        ObjectId::class to ObjectIdSerializer,
        BigDecimal::class to BigDecimalSerializer,
        ByteArray::class to ByteArraySerializer,
        Date::class to DateSerializer,
        UUID::class to UUIDSerializer
    )
)
