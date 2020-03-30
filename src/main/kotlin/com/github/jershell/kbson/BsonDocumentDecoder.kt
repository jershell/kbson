package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.Decoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.internal.NamedValueDecoder
import org.bson.*
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.DecoderContext
import org.bson.types.Decimal128
import org.bson.types.ObjectId

private data class ObjectPropertiesIndexState(
        var currentIndex: Int = 0,
        var count: Int = 0
)

private data class MapElement(
        val names: List<String>,
        val values: List<BsonValue>
) {
    var state: STATE = STATE.NAME
}

@InternalSerializationApi
@Deprecated("Use FlexibleDecoder")
class BsonDocumentDecoder(
        reader: BsonReader,
        override val context: SerialModule,
        private val configuration: Configuration
) : NamedValueDecoder(), Decoder {
    private val mapStack = mutableListOf<MapElement>()
    private val listStack = mutableListOf<BsonArray>()
    private val objectStateStack = mutableListOf<ObjectPropertiesIndexState>()
    private val document: BsonDocument = BsonDocumentCodec().decode(reader, DecoderContext.builder().build())
    var structuresKindStack = mutableListOf<SerialKind>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        if (deserializer !is PolymorphicSerializer<*>) {
            return deserializer.deserialize(this)
        }
        val cTag = currentTagOrNull
        val path = if (cTag != null) currentTag + "." + configuration.classDiscriminator else configuration.classDiscriminator
        structuresKindStack.add(deserializer.descriptor.kind)
        val type = document.getValueByPath(path).asString().value
        val actualSerializer = deserializer.findPolymorphicSerializer(this, type) as KSerializer<T>
        if (cTag != null && document.hasPath(cTag)) {
            return BsonDocumentDecoder(document.get(cTag)!!.asDocument().asBsonReader(), context, configuration).decode(actualSerializer)
        }
        return BsonDocumentDecoder(document.asBsonReader(), context, configuration).decode(actualSerializer)
    }

    private fun extractField(tag: String) = tag.split(".").last()

    private fun BsonDocument.hasPath(tag: String): Boolean {
        var container: BsonValue? = this
        var mapIndex = 0
        tag.split(".").forEachIndexed { index, descIdx ->
            val type = container?.bsonType
            val keyKind = structuresKindStack[index]
            // translate key map by tag[MapReadState.Value]
            val key = if (keyKind == StructureKind.MAP) {
                val idx = descIdx.toInt() / 2
                mapIndex++
                val pos = mapIndex - 1
                mapStack[pos].names[idx]
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
                val idx = descIdx.toInt() / 2
                mapIndex++
                val pos = mapIndex - 1
                mapStack[pos].names[idx]
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
        val hasValueByPath = document.hasPath(tag)

        return when {
            hasValueByPath -> {
                val isNull = document.getValueByPath(tag).bsonType == BsonType.NULL
                if (isNull && structuresKindStack.lastOrNull() == StructureKind.MAP) {
                    when (mapStack.last().state) {
                        STATE.NAME -> {
                            mapStack.last().state = STATE.VALUE
                        }
                        STATE.VALUE -> {
                            mapStack.last().state = STATE.NAME
                        }
                    }
                }
                !isNull
            }
            else -> false
        }
    }

    override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int {
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
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> decodeMapKey(tag).toLong()
                    STATE.VALUE -> document.getValueByPath(tag).asDateTime().value
                }
            }
            else ->  document.getValueByPath(tag).asDateTime().value
        }
    }

    fun decodeTaggedDecimal128(): Decimal128 {
        val tag: String = this.currentTag
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> Decimal128(decodeMapKey(tag).toBigDecimal())
                    STATE.VALUE -> document.getValueByPath(tag).asDecimal128().value
                }
            }
            else ->  document.getValueByPath(tag).asDecimal128().value
        }
    }

    fun decodeByteArray(): ByteArray {
        val tag: String = this.currentTag
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> throw SerializationException("ByteArray is not supported as a key of map")
                    STATE.VALUE -> document.getValueByPath(tag).asBinary().data
                }
            }
            else ->  document.getValueByPath(tag).asBinary().data
        }
    }

    fun decodeObjectId(): ObjectId {
        val tag: String = this.currentTag
        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (mapStack.last().state) {
                    STATE.NAME -> ObjectId(decodeMapKey(tag))
                    STATE.VALUE -> decodeMapValue(tag).asObjectId().value
                }
            }
            else -> document.getValueByPath(tag).asObjectId().value
        }
    }

    override fun decodeCollectionSize(desc: SerialDescriptor): Int {
        return when (desc.kind) {
            StructureKind.MAP -> mapStack.last().names.size
            StructureKind.LIST -> listStack.last().size
            else -> super.decodeCollectionSize(desc)
        }
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        if (desc.kind !is UnionKind) {
            structuresKindStack.add(desc.kind as StructureKind)

            val path = currentTagOrNull ?: ""

            when (desc.kind as StructureKind) {
                is StructureKind.CLASS -> {
                    objectStateStack.add(ObjectPropertiesIndexState(0, desc.elementsCount))
                }
                is StructureKind.MAP -> {
                    val nextMap = document.getValueByPath(path).asDocument()
                    mapStack.add(MapElement(
                            names = nextMap.keys.toList(),
                            values = nextMap.values.toList()
                    ))
                }
                is StructureKind.LIST -> {
                    listStack.add(document.getValueByPath(path).asArray())
                }
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
            is UnionKind -> READ_ALL
            StructureKind.CLASS -> decodeObjectElementIndex(desc)
            StructureKind.MAP, StructureKind.LIST -> READ_ALL
            else -> throw SerializationException("${desc.kind} unsupported")
        }
    }

    private fun decodeObjectElementIndex(desc: SerialDescriptor): Int {
        val state = objectStateStack.last()
        while (true) {
            val idx = state.currentIndex
            if (idx == state.count) {
                return READ_DONE
            }

            if (desc.isElementOptional(idx)) {
                val tag = desc.getTag(idx)
                if (!document.hasPath(tag)) {
                    state.currentIndex++
                    continue
                }
            }

            state.currentIndex = state.currentIndex + 1
            return idx
        }
    }

    private fun decodeMapKey(tag: String): String {
        val map = mapStack.last()
        val idx = extractField(tag).toInt() / 2
        map.state = STATE.VALUE
        return map.names[idx]
    }

    private fun decodeMapValue(tag: String): BsonValue {
        val map = mapStack.last()
        val idx = extractField(tag).toInt() / 2
        map.state = STATE.NAME
        return map.values[idx]
    }
}
