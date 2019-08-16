package com.github.jershell.kbson

import kotlinx.serialization.AbstractSerialFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decode
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import org.bson.*
import org.bson.codecs.*

class KBson(override val context: SerialModule = EmptyModule, private val configuration: Configuration = Configuration()) : AbstractSerialFormat(context) {

    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): BsonDocument {

        val doc = BsonDocument()
        val writer = BsonDocumentWriter(doc)

        serializer.serialize(Encoder(writer, configuration), obj)
        writer.flush()

        return doc
    }


    fun <T> parse(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T {
        return Decoder(doc, context, configuration).decode(deserializer)
    }

    companion object {
        val default = KBson()
    }
}

fun BsonDocument.toDocument(): Document {
    return DocumentCodec().decode(this.asBsonReader(), DecoderContext.builder().build())
}