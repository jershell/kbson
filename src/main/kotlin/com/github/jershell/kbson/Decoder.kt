package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.Decoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.internal.LinkedHashMapClassDesc
import org.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId

private data class ObjectPropertiesIndexState(
        var currentIndex: Int = -1,
        var count: Int = 0
)

private data class MapElement(
        val names: List<String>,
        val values: List<BsonValue>
) {
    var state: STATE = STATE.NAME
}

class Decoder(
        private val document: BsonDocument,
        override val context: SerialModule,
        private val configuration: Configuration
) : NamedValueDecoder(), Decoder {
    private val mapStack = mutableListOf<MapElement>()
    private val listStack = mutableListOf<BsonArray>()
    private val objectStateStack = mutableListOf<ObjectPropertiesIndexState>()
    var structuresKindStack = mutableListOf<StructureKind>()

    private fun extractField(tag: String) = tag.split(".").last()

    private fun BsonDocument.hasPath(tag: String): Boolean {
        var container: BsonValue? = this
        var mapIndex = 0
        tag.split(".").forEachIndexed { index, descIdx ->
            val type = container?.bsonType
            val keyKind = structuresKindStack[index]
            val key = if (keyKind == StructureKind.MAP) {
                mapIndex++
                mapStack[mapIndex - 1].names.get(descIdx.toInt())
            } else descIdx

            if (container == null) {
                return false
            }

            container = when (type) {
                BsonType.DOCUMENT -> container?.asDocument()?.get(key)
                BsonType.ARRAY -> container?.asArray()?.get(key.toInt())
                else -> throw SerializationException("input does not have value by the path $tag")
            }
        }
        return container != null
    }

    private fun BsonDocument.getValueByPath(tag: String): BsonValue {
        var container: BsonValue = this
        var mapIndex = 0
        tag.split(".").forEachIndexed { index, descIdx ->
            val type = container.bsonType
            val keyKind = structuresKindStack[index]

            // translate key map by tag[MapReadState.Value]
            val key = if (keyKind == StructureKind.MAP) {
                mapIndex++
                val pos = mapIndex - 1
                // Since map tag value has offset + 1 from map tag names
                // but now necessary name
                mapStack[pos].names.get(descIdx.toInt() - 1)
            } else descIdx


            container = when (type) {
                BsonType.DOCUMENT -> container.asDocument().get(key) ?: throw MissingFieldException(tag)
                BsonType.ARRAY -> container.asArray().get(key.toInt()) ?: throw MissingFieldException(tag)
                else -> throw MissingFieldException("input does not have value by the path $tag")
            }
        }
        return container
    }

    override fun decodeTaggedNotNullMark(tag: String): Boolean {
        val hasValue = document.hasPath(tag)
        return when {
            hasValue -> !document.getValueByPath(tag).isNull
            else -> false
        }
    }

    override fun decodeTaggedEnum(tag: String, enumDescription: EnumDescriptor): Int {
        val name = document.getValueByPath(tag).asString().value
        val value = enumDescription.getElementIndex(name)
        return if (value == CompositeDecoder.UNKNOWN_NAME) {
            throw SerializationException("Enum has unknown value $name")
        } else value
    }

    override fun decodeTaggedChar(tag: String): Char {

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).first()
                    STATE.VALUE -> {
                        val r = decodeMapValue(tag)
                        r.asSymbol().symbol.first()
                    }
                }
            }
            else -> document.getValueByPath(tag).asSymbol().symbol.first()
        }
    }

    override fun decodeTaggedString(tag: String): String {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag)
                    STATE.VALUE -> decodeMapValue(tag).asString().value
                }
            }
            else -> document.getValueByPath(tag).asString().value
        }
    }

    override fun decodeTaggedInt(tag: String): Int {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toInt()
                    STATE.VALUE -> decodeMapValue(tag).asInt32().value
                }
            }
            else -> document.getValueByPath(tag).asInt32().value
        }
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toBoolean()
                    STATE.VALUE -> decodeMapValue(tag).asBoolean().value
                }
            }
            else -> document.getValueByPath(tag).asBoolean().value
        }
    }

    override fun decodeTaggedDouble(tag: String): Double {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toDouble()
                    STATE.VALUE -> decodeMapValue(tag).asDouble().value
                }
            }
            else -> document.getValueByPath(tag).asDouble().value
        }
    }

    override fun decodeTaggedLong(tag: String): Long {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toLong()
                    STATE.VALUE -> decodeMapValue(tag).asInt64().value
                }
            }
            else -> document.getValueByPath(tag).asInt64().value
        }
    }

    override fun decodeTaggedFloat(tag: String): Float {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toFloat()
                    STATE.VALUE -> decodeMapValue(tag).asDouble().value.toFloat()
                }
            }
            else -> document.getValueByPath(tag).asDouble().value.toFloat()
        }
    }

    override fun decodeTaggedByte(tag: String): Byte {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toByte()
                    STATE.VALUE -> decodeMapValue(tag).asInt32().value.toByte()
                }
            }
            else -> document.getValueByPath(tag).asInt32().value.toByte()
        }
    }

    override fun decodeTaggedShort(tag: String): Short {
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toShort()
                    STATE.VALUE -> decodeMapValue(tag).asInt32().value.toShort()
                }
            }
            else -> document.getValueByPath(tag).asInt32().value.toShort()
        }
    }

    fun decodeTaggedDateTime(): Long {
        val tag: String = this.currentTag
        return document.getValueByPath(tag).asDateTime().value
    }

    fun decodeTaggedDecimal128(): Decimal128 {
        val tag: String = this.currentTag
        return document.getValueByPath(tag).asDecimal128().value
    }

    fun decodeByteArray(): ByteArray {
        val tag: String = this.currentTag
        return document.getValueByPath(tag).asBinary().data
    }

    fun decodeObjectId(): ObjectId {
        val tag: String = this.currentTag
        return document.getValueByPath(tag).asObjectId().value
    }

    override fun decodeCollectionSize(desc: SerialDescriptor): Int {
        return when (desc) {
            is LinkedHashMapClassDesc -> mapStack.last().names.size
            is ArrayListClassDesc -> listStack.last().size
            else -> super.decodeCollectionSize(desc)
        }
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        structuresKindStack.add(desc.kind as StructureKind)

        val path = currentTagOrNull ?: ""

        when (desc.kind as StructureKind) {
            is StructureKind.CLASS -> {
                objectStateStack.add(ObjectPropertiesIndexState(-1, desc.elementsCount))
            }
            is StructureKind.MAP -> {
                val nextMap = document.getValueByPath(path).asDocument()
                mapStack.add(MapElement(
                        names = nextMap.keys.toList(),
                        values = listOf<BsonValue>(BsonInt32(-1)) + nextMap.values.toList()
                ))
            }
            is StructureKind.LIST -> {
                listStack.add(document.getValueByPath(path).asArray())
            }
        }

        return super.beginStructure(desc, *typeParams)
    }

    override fun endStructure(desc: SerialDescriptor) {
        structuresKindStack.removeAt(structuresKindStack.lastIndex)

        when {
            desc.kind == StructureKind.MAP -> mapStack.removeAt(mapStack.lastIndex)
            desc.kind == StructureKind.LIST -> listStack.removeAt(listStack.lastIndex)
            desc.kind == StructureKind.CLASS -> objectStateStack.removeAt(objectStateStack.lastIndex)
        }

        if (structuresKindStack.lastOrNull() == StructureKind.MAP && mapStack.last().state == STATE.VALUE) {
            mapStack.last().state = STATE.NAME
        }
        super.endStructure(desc)
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        return when (desc.kind) {
            StructureKind.CLASS -> decodeObjectElementIndex(desc)
            StructureKind.MAP, StructureKind.LIST -> super.decodeElementIndex(desc)
            else -> throw SerializationException("${desc.kind} unsupported")
        }
    }

    private fun decodeObjectElementIndex(desc: SerialDescriptor): Int {
        val state = objectStateStack.last()
        while (state.currentIndex < state.count - 1) {
            state.currentIndex++
            if (desc.isElementOptional(state.currentIndex)) {
                val tag = desc.getTag(state.currentIndex)
                if (!document.hasPath(tag) && ((state.currentIndex + 1) < state.count)) {
                    state.currentIndex++
                }
            }
            return state.currentIndex
        }
        return READ_DONE
    }

    private fun decodeMapKey(tag: String): String {
        val map = mapStack.last()
        val key = extractField(tag).toInt()
        map.state = STATE.VALUE
        return map.names[key]
    }

    private fun decodeMapValue(tag: String): BsonValue {
        val map = mapStack.last()
        val key = extractField(tag).toInt()
        map.state = STATE.NAME
        return map.values[key]
    }
}