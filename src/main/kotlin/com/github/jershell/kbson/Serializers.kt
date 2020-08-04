package com.github.jershell.kbson

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.serializersModuleOf
import org.bson.BsonType
import org.bson.UuidRepresentation
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class NonEncodeNull


@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder as BsonEncoder
        encoder.encodeDateTime(value.time)
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
            else -> throw SerializationException("Unknown decoder type")
        }
    }
}


@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("BigDecimalSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder as BsonEncoder
        encoder.encodeDecimal128(Decimal128(value))
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
            else -> throw SerializationException("Unknown decoder type")
        }
    }
}


@Serializer(forClass = ByteArray::class)
object ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ByteArraySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder as BsonEncoder
        encoder.encodeByteArray(value)
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): ByteArray {
        return when (decoder) {
            is FlexibleDecoder -> {
                decoder.reader.readBinaryData().data
            }
            else -> throw SerializationException("Unknown decoder type")
        }

    }
}


@Serializer(forClass = ObjectId::class)
object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ObjectIdSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder as BsonEncoder
        encoder.encodeObjectId(value)
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
            else -> throw SerializationException("Unknown decoder type")
        }

    }
}

@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder as BsonEncoder
        encoder.encodeUUID(value, UuidRepresentation.STANDARD)
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

val DefaultModule = SerializersModule {
    contextual(ObjectId::class, ObjectIdSerializer)
    contextual(BigDecimal::class, BigDecimalSerializer)
    contextual(ByteArray::class, ByteArraySerializer)
    contextual(Date::class, DateSerializer)
    contextual(UUID::class, UUIDSerializer)
}
