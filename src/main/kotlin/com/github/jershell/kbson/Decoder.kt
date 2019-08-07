package com.github.jershell.kbson

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.Decoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.LinkedHashMapClassDesc
import org.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId

class Decoder(val document: BsonDocument, override val context: SerialModule) : NamedValueDecoder(), Decoder {

    private var state = STATE.VALUE
    var structuresKindStack = mutableListOf<SerialKind>()
    private val mapsStack = mutableListOf<Pair<List<String>, List<BsonValue>>>()
    private val listStack = mutableListOf<List<BsonValue>>()

    private fun extractField(tag: String) = tag.split(".").last()

    fun BsonDocument.getByPath(path: String): BsonDocument {
        var target = this
        val instructions = path.split(".")

        instructions.forEachIndexed { index: Int, key: String ->
            if (instructions.lastIndex == index) {
                return target
            } else {
                val kind = structuresKindStack[index]

                if(kind == StructureKind.MAP) {
                    val decodedValue = decodeMapValue(target, key)
                    if(decodedValue.isDocument) {
                        target = decodedValue.asDocument()
                    } else if(decodedValue.isArray) {
                        target = BsonDocument().apply {
                            decodedValue.asArray().forEachIndexed { idx, value ->
                                put(idx.toString(), value)
                            }
                        }
                    }

                } else {
                    if (target[key]?.isArray == true) {
                        val arr = target.getArray(key)
                        target = BsonDocument().apply {
                            arr.forEachIndexed { idx, value ->
                                put(idx.toString(), value)
                            }
                        }
                    } else if (target[key]?.isDocument == true) {
                        target = target.getDocument(key)
                    } else {
                        throw SerializationException("extract element is fail $path")
                    }
                }

            }
        }
        throw MissingFieldException(path)
    }

    override fun decodeTaggedChar(tag: String): Char {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).first()
                    STATE.VALUE -> decodeMapValue(doc, field).asSymbol().symbol.first()
                }
            }
            else -> doc.getValue(field).asSymbol().symbol.first()
        }
    }

    override fun decodeTaggedValue(tag: String): Any {
        return super.decodeTaggedValue(tag)
    }

    override fun decodeTaggedString(tag: String): String {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field)
                    STATE.VALUE -> decodeMapValue(doc, field).asString().value
                }
            }
            else -> doc.getString(field).value
        }
    }

    override fun decodeTaggedInt(tag: String): Int {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {

                return when (state) {

                    STATE.NAME -> decodeMapKey(doc, field).toInt()
                    STATE.VALUE -> decodeMapValue(doc, field).asInt32().value
                }
            }
            else -> doc.getInt32(field).value
        }
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toBoolean()
                    STATE.VALUE -> decodeMapValue(doc, field).asBoolean().value
                }
            }
            else -> doc.getBoolean(field).value
        }
    }

    override fun decodeTaggedDouble(tag: String): Double {

        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toDouble()
                    STATE.VALUE -> decodeMapValue(doc, field).asDouble().value
                }
            }
            else -> doc.getDouble(field).value
        }
    }

    override fun decodeTaggedLong(tag: String): Long {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toLong()
                    STATE.VALUE -> decodeMapValue(doc, field).asInt64().value
                }
            }
            else -> doc.getInt64(field).value
        }
    }

    override fun decodeTaggedFloat(tag: String): Float {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toFloat()
                    STATE.VALUE -> decodeMapValue(doc, field).asDouble().value.toFloat()
                }
            }
            else -> doc.getDouble(field).value.toFloat()
        }
    }

    override fun decodeTaggedByte(tag: String): Byte {
        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toByte()
                    STATE.VALUE -> decodeMapValue(doc, field).asInt32().value.toByte()
                }
            }
            else -> doc.getInt32(field).value.toByte()
        }
    }

    override fun decodeTaggedShort(tag: String): Short {

        val doc = document.getByPath(tag)
        val field = extractField(tag)

        return when {
            structuresKindStack.lastOrNull() == StructureKind.MAP -> {
                return when (state) {
                    STATE.NAME -> decodeMapKey(doc, field).toShort()
                    STATE.VALUE -> decodeMapValue(doc, field).asInt32().value.toShort()
                }
            }
            else -> doc.getInt32(field).value.toShort()
        }

    }

    fun decodeTaggedDateTime(): Long {
        val tag: String = this.currentTag
        return document.getByPath(tag).getDateTime(extractField(tag)).value
    }

    fun decodeTaggedDecimal128(): Decimal128 {
        val tag: String = this.currentTag
        return document.getByPath(tag).getDecimal128(extractField(tag)).value
    }

    fun decodeByteArray(): ByteArray {
        val tag: String = this.currentTag
        return document.getByPath(tag).getBinary(extractField(tag)).data
    }

    fun decodeObjectId(): ObjectId {
        val tag: String = this.currentTag
        return document.getByPath(tag).getObjectId(extractField(tag)).value
    }

    override fun decodeCollectionSize(desc: SerialDescriptor): Int {
        val tag: String = this.currentTag
        return when (desc) {
            is LinkedHashMapClassDesc -> {
                if (structuresKindStack.lastOrNull() == desc.kind) {
                    mapsStack.last().first.size
                } else {
                    document.getByPath(tag).getDocument(extractField(tag)).size
                }
            }
            is ArrayListClassDesc -> listStack.last().size
            else -> super.decodeCollectionSize(desc)
        }
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val lastStructuresKind = structuresKindStack.lastOrNull()

        structuresKindStack.add(desc.kind)

        when (desc.kind) {
            is StructureKind.MAP -> {
                val path = currentTagOrNull

                if (path != null) {
                    val field = extractField(path)
                    val doc = document.getByPath(path)

                    val target = if (lastStructuresKind == StructureKind.MAP) {
                        decodeMapValue(doc, field).asDocument()
                    } else {
                        doc.getDocument(field)
                    }

                    val values = mutableListOf<BsonValue>(BsonInt32(-1)).apply {
                        addAll(target.values.toList())
                    }

                    mapsStack.add(Pair(
                            target.keys.toList(),
                            values
                    ))
                }

                state = STATE.NAME
            }
            is StructureKind.LIST -> {
                val path = currentTagOrNull
                if (path != null) {
                    val field = extractField(path)
                    val doc = document.getByPath(path)
                    if(lastStructuresKind == StructureKind.MAP) {
                        listStack.add(decodeMapValue(doc, field).asArray())
                    } else {
                        listStack.add(doc.getArray(field))
                    }
                }
            }
        }

        return super.beginStructure(desc, *typeParams)
    }

    override fun endStructure(desc: SerialDescriptor) {
        structuresKindStack.removeAt(structuresKindStack.lastIndex)

        if (desc.kind == StructureKind.MAP) {
            mapsStack.removeAt(mapsStack.lastIndex)
        } else if (desc.kind == StructureKind.LIST) {
            listStack.removeAt(listStack.lastIndex)
        }

        super.endStructure(desc)
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        return super.decodeElementIndex(desc)
    }

    fun decodeMapKey(doc: BsonDocument, field: String): String {
        state = STATE.VALUE
        val idx = field.toInt()
        return mapsStack.last().first[idx]
    }

    fun decodeMapValue(doc: BsonDocument, field: String): BsonValue {
        state = STATE.NAME
        val idx = field.toInt()
        return mapsStack.last().second[idx]
    }
}