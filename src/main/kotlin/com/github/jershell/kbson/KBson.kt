package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import org.bson.*
import org.bson.codecs.*



class KBson(override val serializersModule: SerializersModule = DefaultModule, private val configuration: Configuration = Configuration()) : SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): BsonDocument {
        val doc = BsonDocument()
        val writer = BsonDocumentWriter(doc)

        serializer.serialize(BsonEncoder(writer, serializersModule, configuration), obj)
        writer.flush()

        return doc
    }

    fun <T> parse(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T {
        return BsonFlexibleDecoder((doc.asBsonReader() as AbstractBsonReader), serializersModule, configuration).decodeSerializableValue(deserializer)
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, doc: ByteArray): T {
        return BsonFlexibleDecoder((RawBsonDocument(doc).asBsonReader() as AbstractBsonReader), serializersModule, configuration).decodeSerializableValue(deserializer)
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T {
        return BsonFlexibleDecoder((doc.asBsonReader() as AbstractBsonReader), serializersModule, configuration).decodeSerializableValue(deserializer)
    }

    fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        return this.stringify(serializer, obj).toByteArray()
    }

    companion object {
        val default = KBson()
    }
}

fun BsonDocument.toDocument(): Document {
    return DocumentCodec().decode(this.asBsonReader(), DecoderContext.builder().build())
}

fun BsonDocument.toByteArray(): ByteArray {
    return RawBsonDocumentCodec()
            .decode(this.asBsonReader(), DecoderContext.builder().build())
            .byteBuffer
            .array()
}
