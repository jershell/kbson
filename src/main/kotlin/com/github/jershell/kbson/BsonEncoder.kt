package com.github.jershell.kbson

import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import org.bson.BsonBinary
import org.bson.BsonWriter
import org.bson.UuidRepresentation
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.util.UUID


open class BsonEncoder(
        private val writer: BsonWriter,
        override val serializersModule: SerializersModule,
        private val configuration: Configuration
) : AbstractEncoder() {

    private var state = STATE.VALUE
    private var hasBegin = false // for UnionKind
    private var stateMap = StateMap()
    private var deferredKeyName: String? = null

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
            configuration.encodeDefaults

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        when (descriptor.kind) {
            StructureKind.LIST -> writer.writeStartArray()
            StructureKind.CLASS -> {
                if (hasBegin) {
                    hasBegin = false
                } else {
                    writer.writeStartDocument()
                }
            }
            StructureKind.OBJECT -> {
                if (hasBegin) {
                    hasBegin = false
                } else {
                    writer.writeStartDocument()
                }
            }
            is PolymorphicKind -> {
                writer.writeStartDocument()
                writer.writeName(descriptor.classDiscriminator())
                hasBegin = true
            }
            StructureKind.MAP -> {
                writer.writeStartDocument()
                stateMap = StateMap()
            }
            else -> throw SerializationException("Primitives are not supported at top-level")
        }
        return super.beginStructure(descriptor)
    }

    private fun SerialDescriptor.classDiscriminator(): String {
        for (annotation in this.annotations) {
            if (annotation is BsonClassDiscriminator) {
                return annotation.discriminator
            }
        }
        return configuration.classDiscriminator
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST -> writer.writeEndArray()
            is StructureKind.MAP, StructureKind.CLASS, StructureKind.OBJECT -> writer.writeEndDocument()
/*            SerialKind.ENUM -> TODO()
            SerialKind.CONTEXTUAL -> TODO()
            PrimitiveKind.BOOLEAN -> TODO()
            PrimitiveKind.BYTE -> TODO()
            PrimitiveKind.CHAR -> TODO()
            PrimitiveKind.SHORT -> TODO()
            PrimitiveKind.INT -> TODO()
            PrimitiveKind.LONG -> TODO()
            PrimitiveKind.FLOAT -> TODO()
            PrimitiveKind.DOUBLE -> TODO()
            PrimitiveKind.STRING -> TODO()
            PolymorphicKind.SEALED -> TODO()
            PolymorphicKind.OPEN -> TODO()*/
        }
    }

    override fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
        when {
            deferredKeyName != null && value == null -> {
                deferredKeyName = null
                // and nothing
            }
            deferredKeyName != null && value != null -> {
                writer.writeName(deferredKeyName)
                deferredKeyName = null
                super.encodeNullableSerializableValue(serializer, value)
            }
            else -> super.encodeNullableSerializableValue(serializer, value)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        when (descriptor.kind) {
            is StructureKind.CLASS -> {
                val name = descriptor.getElementName(index)

                val elemDesc = try {
                    descriptor.getElementDescriptor(index)
                } catch (e: IndexOutOfBoundsException) {
                    null
                }
                if (elemDesc?.isNullable != false) {
                    val ann =
                            configuration.nonEncodeNull || descriptor.getElementAnnotations(index)
                                    .any { it is NonEncodeNull }
                    if (ann) {
                        deferredKeyName = name
                    }
                }

                if (deferredKeyName == null) {
                    writer.writeName(name)
                }
            }
            is StructureKind.MAP -> {
                state = stateMap.next()
            }

/*            SerialKind.ENUM -> TODO()
            SerialKind.CONTEXTUAL -> TODO()
            PrimitiveKind.BOOLEAN -> TODO()
            PrimitiveKind.BYTE -> TODO()
            PrimitiveKind.CHAR -> TODO()
            PrimitiveKind.SHORT -> TODO()
            PrimitiveKind.INT -> TODO()
            PrimitiveKind.LONG -> TODO()
            PrimitiveKind.FLOAT -> TODO()
            PrimitiveKind.DOUBLE -> TODO()
            PrimitiveKind.STRING -> TODO()
            StructureKind.LIST -> TODO()
            StructureKind.OBJECT -> TODO()
            PolymorphicKind.SEALED -> TODO()
            PolymorphicKind.OPEN -> TODO()*/
        }
        return true
    }

    override fun encodeNull() {
        writer.writeNull()
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val value = enumDescriptor.getElementName(index)
        if (state == STATE.NAME) {
            writer.writeName(value)
        } else {
            writer.writeString(value)
        }
    }

    override fun encodeString(value: String) {
        when (state) {
            STATE.NAME -> encodeStructName(value)
            STATE.VALUE -> writer.writeString(value)
        }
    }

    override fun encodeInt(value: Int) {
        when (state) {
            STATE.VALUE -> writer.writeInt32(value)
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeDouble(value: Double) {
        when (state) {
            STATE.VALUE -> writer.writeDouble(value)
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeFloat(value: Float) {
        when (state) {
            STATE.VALUE -> writer.writeDouble(value.toDouble())
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeLong(value: Long) {
        when (state) {
            STATE.VALUE -> writer.writeInt64(value)
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeChar(value: Char) {
        when (state) {
            STATE.VALUE -> writer.writeSymbol(value.toString())
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeBoolean(value: Boolean) {
        when (state) {
            STATE.VALUE -> writer.writeBoolean(value)
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeByte(value: Byte) {
        when (state) {
            STATE.VALUE -> writer.writeInt32(value.toInt())
            STATE.NAME -> encodeStructName(value)
        }
    }

    override fun encodeShort(value: Short) {
        when (state) {
            STATE.VALUE -> writer.writeInt32(value.toInt())
            STATE.NAME -> encodeStructName(value)
        }
    }

    fun encodeDateTime(value: Long) {
        when (state) {
            STATE.VALUE -> writer.writeDateTime(value)
            STATE.NAME -> encodeStructName(value.toString())
        }
    }

    fun encodeObjectId(value: ObjectId) {
        when (state) {
            STATE.VALUE -> writer.writeObjectId(value)
            STATE.NAME -> encodeStructName(value.toString())
        }
    }

    fun encodeDecimal128(value: Decimal128) {
        when (state) {
            STATE.VALUE -> writer.writeDecimal128(value)
            STATE.NAME -> encodeStructName(value.toString())
        }
    }

    fun encodeByteArray(value: ByteArray) {
        when (state) {
            STATE.VALUE -> writer.writeBinaryData(BsonBinary(value))
            // I think we can use base64, but files can be big
            STATE.NAME -> throw SerializationException("ByteArray is not supported as a key of map")
        }
    }

    fun encodeUUID(value: UUID, uuidRepresentation: UuidRepresentation = UuidRepresentation.STANDARD) {
        when (state) {
            STATE.VALUE -> writer.writeBinaryData(BsonBinary(value, uuidRepresentation))
            // I think we can use base64, but files can be big
            STATE.NAME -> throw SerializationException("UUID is not supported as a key of map")
        }
    }

    private fun encodeStructName(value: Any) {
        writer.writeName(value.toString())
        state = STATE.VALUE
    }
}