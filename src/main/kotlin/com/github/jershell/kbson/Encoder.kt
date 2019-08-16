package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.Encoder
import kotlinx.serialization.internal.LinkedHashMapClassDesc
import kotlinx.serialization.internal.PrimitiveDescriptor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.internal.EnumDescriptor
import org.bson.BsonBinary
import org.bson.BsonDocumentWriter
import org.bson.types.Decimal128
import org.bson.types.ObjectId



class Encoder(private val writer: BsonDocumentWriter, private val configuration: Configuration) : ElementValueEncoder(), Encoder {

    private var state = STATE.VALUE
    private var stateMap = StateMap()

    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = configuration.encodeDefaults

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        when (desc.kind as StructureKind) {
            is StructureKind.LIST -> writer.writeStartArray()
            is StructureKind.MAP -> {
                writer.writeStartDocument()
                stateMap = StateMap()
            }
            is StructureKind.CLASS -> writer.writeStartDocument()
        }
        return super.beginStructure(desc, *typeParams)
    }

    override fun endStructure(desc: SerialDescriptor) {
        when (desc.kind) {
            is StructureKind.LIST -> writer.writeEndArray()
            is StructureKind.MAP, StructureKind.CLASS -> writer.writeEndDocument()
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        when (desc.kind) {
            is StructureKind.CLASS -> {
                val name = desc.getElementName(index)
                writer.writeName(name)
            }
            is StructureKind.MAP -> {
                val mapDesc = desc as LinkedHashMapClassDesc
                if (mapDesc.keyDescriptor !is PrimitiveDescriptor) {
                    throw SerializationException("map key name is not primitive")
                }
                state = stateMap.next()
            }
        }
        return true
    }

    override fun encodeNull() {
        writer.writeNull()
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        writer.writeString(enumDescription.getElementName(ordinal))
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

    override fun encodeUnit() {
        writer.writeNull()
    }

    override fun encodeShort(value: Short) {
        when (state) {
            STATE.VALUE -> writer.writeInt32(value.toInt())
            STATE.NAME -> encodeStructName(value)
        }
    }

    fun encodeDateTime(value: Long) {
        writer.writeDateTime(value)
    }

    fun encodeObjectId(value: ObjectId) {
        writer.writeObjectId(value)
    }

    fun encodeDecimal128(value: Decimal128) {
        writer.writeDecimal128(value)
    }

    fun encodeByteArray(value: ByteArray) {
        writer.writeBinaryData(BsonBinary(value))
    }

    private fun encodeStructName(value: Any) {
        writer.writeName(value.toString())
        state = STATE.VALUE
    }
}