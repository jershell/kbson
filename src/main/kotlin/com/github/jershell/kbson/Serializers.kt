package com.github.jershell.kbson

import com.github.jershell.kbson.Encoder as BsonEncoder
import com.github.jershell.kbson.Decoder as BsonDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Encoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.util.Date

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("DateSerializer")

    override fun serialize(encoder: Encoder, obj: Date) {
        encoder as BsonEncoder
        encoder.encodeDateTime(obj.time)
    }

    override fun deserialize(decoder: Decoder): Date {
        decoder as BsonDecoder
        println("___$decoder")
        return Date(decoder.decodeTaggedDateTime())
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
        decoder as BsonDecoder
        return decoder.decodeTaggedDecimal128().bigDecimalValue()
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
        decoder as BsonDecoder
        return decoder.decodeByteArray()
    }
}


@Serializer(forClass = ObjectId::class)
object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("ObjectIdSerializer")

    override fun serialize(encoder: Encoder, obj: ObjectId) {
        encoder as BsonEncoder
        encoder.encodeObjectId(obj)
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        decoder as BsonDecoder
        return decoder.decodeObjectId()
    }
}