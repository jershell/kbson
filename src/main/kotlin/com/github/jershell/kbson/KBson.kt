package com.github.jershell.kbson

import kotlinx.serialization.AbstractSerialFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decode
import kotlinx.serialization.modules.SerialModule
import org.bson.*
import org.bson.codecs.*
import java.nio.ByteBuffer

class KBson(override val context: SerialModule = DefaultModule, private val configuration: Configuration = Configuration()) : AbstractSerialFormat(context) {

    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): BsonDocument {
        val doc = BsonDocument()
        val writer = BsonDocumentWriter(doc)

        serializer.serialize(BsonEncoder(writer, context, configuration), obj)
        writer.flush()

        return doc
    }

    fun <T> parse(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T {
        return BsonDocumentDecoder(doc.asBsonReader(), context, configuration).decode(deserializer)
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, doc: ByteArray): T {
        return BsonDecoder(BsonBinaryReader(ByteBuffer.wrap(doc)), context, configuration).decode(deserializer)
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T {
        return BsonDecoder(doc.asBsonReader(), context, configuration).decode(deserializer)
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
