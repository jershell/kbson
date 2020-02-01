package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.modules.SerialModule
import org.bson.*

@Deprecated("Use FlexibleDecoder")
open class BsonDecoder(
        val reader: BsonReader,
        override val context: SerialModule,
        private val configuration: Configuration
) : ElementValueDecoder() {
    var idx = 0
    private val abReader: AbstractBsonReader
        get() = reader as AbstractBsonReader

    private val cState: AbstractBsonReader.State
        get() = abReader.state

    override fun decodeNotNullMark(): Boolean {
        return reader.currentBsonType != BsonType.NULL
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return when (desc.kind) {
            StructureKind.LIST -> ListDocumentDecoder(reader, context, configuration)
            StructureKind.MAP -> MapDocumentDecoder(reader, context, configuration)
            StructureKind.CLASS -> {
                reader.readStartDocument()
                BsonDecoder(reader, context, configuration)
            }
            else -> super.beginStructure(desc, *typeParams)
        }
    }

    override fun decodeBoolean(): Boolean {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toBoolean()
        }
        return reader.readBoolean()
    }

    override fun decodeEnum(enumDescription: SerialDescriptor): Int {
        val name = reader.readString()
        val value = enumDescription.getElementIndex(name)
        return if (value == CompositeDecoder.UNKNOWN_NAME) {
            throw SerializationException("Enum has unknown value $name")
        } else value
    }

    override fun decodeByte(): Byte {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toByte()
        }
        return reader.readInt32().toByte()
    }

    override fun decodeNull(): Nothing? {
        reader.readNull()
        return null
    }

    override fun decodeChar(): Char {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.first()
        }
        return reader.readSymbol().first()
    }

    override fun decodeDouble(): Double {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toDouble()
        }
        return reader.readDouble()
    }

    override fun decodeInt(): Int {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toInt()
        }
        return reader.readInt32()
    }

    override fun decodeShort(): Short {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toShort()
        }
        return reader.readInt32().toShort()
    }

    override fun decodeLong(): Long {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toLong()
        }
        return reader.readInt64()
    }

    override fun decodeFloat(): Float {
        if (cState == AbstractBsonReader.State.NAME) {
            val name = reader.readName()
            return name!!.toFloat()
        }
        return reader.readDouble().toFloat()
    }

    override fun decodeString(): String {
        if (cState == AbstractBsonReader.State.NAME) {
            return reader.readName()
        }
        return reader.readString()
    }

    override fun decodeUnit() {
        reader.readUndefined()
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (cState != AbstractBsonReader.State.VALUE) {
            when (desc.kind) {
                is StructureKind.LIST -> reader.readEndArray()
                is StructureKind.MAP, StructureKind.CLASS -> reader.readEndDocument()
            }
        }
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        if (idx >= desc.elementsCount) return READ_DONE

        if (cState == AbstractBsonReader.State.TYPE) {
            val nextType = reader.readBsonType()
            if (nextType == BsonType.END_OF_DOCUMENT) return READ_DONE

            var expectedName = desc.getElementName(idx)
            val currentName = reader.readName()

            if (expectedName == currentName) {
                return idx
            } else {
                while (expectedName != currentName) {
                    idx++
                    if (idx >= desc.elementsCount) return READ_DONE
                    expectedName = desc.getElementName(idx)
                }
            }
        }
        return idx
    }


    private inner class MapDocumentDecoder(reader: BsonReader, context: SerialModule, configuration: Configuration) : BsonDecoder(reader, context, configuration) {
        private var ind = 0
        private val names = mutableListOf<String>()

        init {
            reader.readStartDocument()
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            if (cState == AbstractBsonReader.State.TYPE) {
                val nextType = reader.readBsonType()
                return if (nextType == BsonType.END_OF_DOCUMENT) READ_DONE else ind++
            }
            return ind
        }
    }

    private inner class ListDocumentDecoder(reader: BsonReader, context: SerialModule, configuration: Configuration)
        : BsonDecoder(reader, context, configuration) {
        init {
            reader.readStartArray()
        }

        private var ind = 0
        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            val nextType = reader.readBsonType()
            return if (nextType == BsonType.END_OF_DOCUMENT) READ_DONE else ind++
        }
    }
}