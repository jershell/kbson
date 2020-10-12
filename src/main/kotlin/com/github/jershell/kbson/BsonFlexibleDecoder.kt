package com.github.jershell.kbson

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.modules.SerializersModule
import org.bson.AbstractBsonReader
import org.bson.AbstractBsonReader.State
import org.bson.BsonType
import org.bson.types.ObjectId

abstract class FlexibleDecoder(
    val reader: AbstractBsonReader,
    override val serializersModule: SerializersModule,
    val configuration: Configuration
) : AbstractDecoder() {

    protected open var alreadyReadId: Any?
        get() = null
        set(_) {
            //do nothing
        }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.CLASS -> {
                val current = reader.currentBsonType
                if (current == null || current == BsonType.DOCUMENT) {
                    reader.readStartDocument()
                }
                BsonFlexibleDecoder(reader, serializersModule, configuration, alreadyReadId)
            }
            StructureKind.MAP -> {
                reader.readStartDocument()
                MapDecoder(reader, serializersModule, configuration)
            }
            StructureKind.LIST -> {
                reader.readStartArray()
                ListDecoder(reader, serializersModule, configuration)
            }
            is PolymorphicKind -> {
                reader.readStartDocument()
                PolymorphismDecoder(reader, serializersModule, configuration, alreadyReadId)
            }
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST -> reader.readEndArray()
            is StructureKind.MAP, StructureKind.CLASS, StructureKind.OBJECT -> reader.readEndDocument()
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return reader.currentBsonType != BsonType.NULL
    }

    override fun decodeBoolean(): Boolean {
        return reader.readBoolean()
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = reader.readString()
        val value = enumDescriptor.getElementIndex(name)
        return if (value == UNKNOWN_NAME) {
            throw SerializationException("Enum has unknown value $name")
        } else {
            value
        }
    }

    override fun decodeByte(): Byte {
        return reader.readInt32().toByte()
    }

    override fun decodeNull(): Nothing? {
        reader.readNull()
        return null
    }

    override fun decodeChar(): Char {
        return reader.readSymbol().first()
    }

    override fun decodeDouble(): Double {
        return reader.readDouble()
    }

    override fun decodeInt(): Int {
        return reader.readInt32()
    }

    override fun decodeShort(): Short {
        return reader.readInt32().toShort()
    }

    override fun decodeLong(): Long {
        return reader.readInt64()
    }

    override fun decodeFloat(): Float {
        return reader.readDouble().toFloat()
    }

    override fun decodeString(): String {
        return reader.readString()
    }
}

class BsonFlexibleDecoder(
    reader: AbstractBsonReader,
    context: SerializersModule,
    configuration: Configuration,
    override var alreadyReadId: Any? = null
) : FlexibleDecoder(reader, context, configuration) {

    //to handle not optional nullable properties
    private var indexesSet: BooleanArray? = null
    private var containsNotOptionalNullable: Boolean? = null
    private var checkNotOptionalNullable = false

    private fun initNotOptionalProperties(desc: SerialDescriptor) {
        if (containsNotOptionalNullable == null) {
            for (i in 0 until desc.elementsCount) {
                if (!desc.isElementOptional(i)) {
                    val nullable =
                        try {
                            desc.getElementDescriptor(i).isNullable
                        } catch (e: Exception) {
                            true
                        }
                    if (nullable) {
                        containsNotOptionalNullable = true
                        break
                    }
                }
            }
            if (containsNotOptionalNullable == null) {
                containsNotOptionalNullable = false
            } else {
                indexesSet = BooleanArray(desc.elementsCount)
            }
        }
    }

    private fun checkNotOptionalProperties(desc: SerialDescriptor): Int {
        //set to null not optional nullable properties if any
        if (containsNotOptionalNullable!!) {
            for (i in 0 until desc.elementsCount) {
                if (indexesSet?.get(i) != true
                    && !desc.isElementOptional(i)
                    && try {
                        desc.getElementDescriptor(i).isNullable
                    } catch (e: Exception) {
                        true
                    }
                ) {
                    checkNotOptionalNullable = true
                    indexesSet!![i] = true
                    return i
                }
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        initNotOptionalProperties(descriptor)
        if (alreadyReadId != null) {
            val idIndex = descriptor.getElementIndex("_id")
            if (idIndex != UNKNOWN_NAME) {
                return idIndex
            } else {
                alreadyReadId = null
            }
        }

        if (reader.state == State.TYPE) {
            reader.readBsonType()
        }
        return when (reader.state) {
            State.NAME -> {
                val currentName = reader.readName()
                val index = descriptor.getElementIndex(currentName)
                if (index == UNKNOWN_NAME) {
                    reader.skipValue()
                    decodeElementIndex(descriptor)
                } else {
                    if (containsNotOptionalNullable!!) {
                        indexesSet!![index] = true
                    }
                    index
                }
            }
            else -> {
                checkNotOptionalProperties(descriptor)
            }
        }
    }

    override fun decodeNotNullMark(): Boolean {
        return !checkNotOptionalNullable && reader.currentBsonType != BsonType.NULL
    }

    override fun decodeNull(): Nothing? {
        if (!checkNotOptionalNullable) {
            reader.readNull()
        }
        return null
    }

    override fun decodeString(): String =
        if (alreadyReadId != null) {
            val result = alreadyReadId
            alreadyReadId = null
            result as? String ?: (result as ObjectId).toString()
        } else {
            super.decodeString()
        }
}

private class PolymorphismDecoder(
    reader: AbstractBsonReader,
    val context: SerializersModule,
    configuration: Configuration,
    override var alreadyReadId: Any? = null
) : FlexibleDecoder(reader, context, configuration) {
    private var decodeCount = 0

    @InternalSerializationApi
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        deserializer.deserialize(BsonFlexibleDecoder(reader, context, configuration, alreadyReadId))

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return when (decodeCount) {
            0 -> {
                if (reader.state == State.TYPE) {
                    reader.readBsonType()
                }
                val fieldName = reader.readName()
                if (fieldName == "_id") {
                    alreadyReadId = when (reader.currentBsonType) {
                        BsonType.OBJECT_ID -> reader.readObjectId()
                        BsonType.STRING -> reader.readString()
                        else -> error("only ObjectId or string are supported as _id for polymorphism decoder ")
                    }
                }
                decodeCount++
            }
            1 -> {
                decodeCount++
            }
            else -> CompositeDecoder.DECODE_DONE
        }
    }
}

private class MapDecoder(
    reader: AbstractBsonReader,
    context: SerializersModule,
    configuration: Configuration
) : FlexibleDecoder(reader, context, configuration) {

    private var index = 0
    private var key: Boolean = false

    override fun decodeBoolean(): Boolean {
        return if (key) {
            reader.readName()!!.toBoolean()
        } else {
            super.decodeBoolean()
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return if (key) {
            enumDescriptor.getElementIndex(reader.readName()!!)
        } else {
            super.decodeEnum(enumDescriptor)
        }
    }

    override fun decodeByte(): Byte {
        return if (key) {
            reader.readName()!!.toByte()
        } else {
            super.decodeByte()
        }
    }

    override fun decodeChar(): Char {
        return if (key) {
            reader.readName()!!.first()
        } else {
            super.decodeChar()
        }
    }

    override fun decodeDouble(): Double {
        return if (key) {
            reader.readName()!!.toDouble()
        } else {
            super.decodeDouble()
        }
    }

    override fun decodeInt(): Int {
        return if (key) {
            reader.readName()!!.toInt()
        } else {
            super.decodeInt()
        }
    }

    override fun decodeShort(): Short {
        return if (key) {
            reader.readName()!!.toShort()
        } else {
            super.decodeShort()
        }
    }

    override fun decodeLong(): Long {
        return if (key) {
            reader.readName()!!.toLong()
        } else {
            super.decodeLong()
        }
    }

    override fun decodeFloat(): Float {
        return if (key) {
            reader.readName()!!.toFloat()
        } else {
            super.decodeFloat()
        }
    }

    override fun decodeString(): String {
        return if (key) {
            reader.readName()
        } else {
            super.decodeString()
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (!key) {
            key = true
            val nextType = reader.readBsonType()
            if (nextType == BsonType.END_OF_DOCUMENT) return CompositeDecoder.DECODE_DONE
        } else {
            key = false
        }
        return index++
    }
}

private class ListDecoder(
    reader: AbstractBsonReader,
    context: SerializersModule,
    configuration: Configuration
) : FlexibleDecoder(reader, context, configuration) {
    private var index = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val nextType = reader.readBsonType()
        return if (nextType == BsonType.END_OF_DOCUMENT) CompositeDecoder.DECODE_DONE else index++
    }
}