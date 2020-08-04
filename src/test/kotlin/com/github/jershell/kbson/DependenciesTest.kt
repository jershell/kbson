package com.github.jershell.kbson

import com.github.jershell.kbson.models.NullableDefaultClass
import com.github.jershell.kbson.models.OptionalClass
import com.github.jershell.kbson.models.Simple
import com.github.jershell.kbson.models.WrapperSet
import com.github.jershell.kbson.models.polymorph.IntMessage
import com.github.jershell.kbson.models.polymorph.Message
import com.github.jershell.kbson.models.polymorph.MessageWrapper
import com.github.jershell.kbson.models.polymorph.StringMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals


@Serializable
data class EBA(val bin: Double)

@Serializable
data class AAA(
        val mmpp: Map<EBA, Byte>
)

class DependenciesTest {
//    @Test
//    fun JsonStringify() {
//        val target = Simple("value_string", 20.5, 0.1f, 42L, '€', true, 42)
//        assertEquals("""{"valueString":"text","valueDouble":42.0,"valueFloat":42.0,"valueInt":42}""", Json.nonstrict.stringify(Simple.serializer(), target))
//    }

    val nonstrictJson = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
    }

    @Test
    fun mapSP() {
        val exp = """
            {"mmpp":[{"bin":2.0},20,{"bin":3.0},30]}
        """.trimIndent()

        val target = AAA(mapOf(EBA(2.0) to 20.toByte(), EBA(3.0) to 30.toByte()))

        val json = Json {
            allowStructuredMapKeys = true
            isLenient = true
            useArrayPolymorphism = false
        }

        assertEquals(exp, json.encodeToString(AAA.serializer(), target))
        assertEquals(target, json.decodeFromString(AAA.serializer(), exp))
    }

    @Test
    fun JsonParse() {
        val target = Simple("text", PI, 42.0f, 42L, '€', true, 42)
        assertEquals(target, nonstrictJson.decodeFromString(Simple.serializer(),
                """
                    {
                    "valueLong": 42,
                    "valueChar": "€",
                    "valueBool": true,
                    "valueString":"text",
                    "valueDouble":$PI,
                    "valueFloat":42.0,
                    "valueInt":42
                    }
                """.trimIndent()))
    }

    @Test
    fun JsonParseNullableClass() {
        val n = OptionalClass()
        val res = nonstrictJson.decodeFromString(OptionalClass.serializer(), """{}""")

        assertEquals(n.reqString, res.reqString)
    }

    @Test
    fun nonFieldParse() {
        val doc = "{}"
        val value = NullableDefaultClass()
        val result = Json.decodeFromString(NullableDefaultClass.serializer(), doc)
        assertEquals(value, result)
    }

    @Test
    fun stringifyPolymorphism() {
        val pModule = SerializersModule {
            polymorphic(Message::class) {
                subclass(StringMessage::class, StringMessage.serializer())
                subclass(IntMessage::class, IntMessage.serializer())
            }
        }

        val mDoc = """{"m":{"type":"com.github.jershell.kbson.models.polymorph.StringMessage","msg":"FortyTwo"}}"""
        val nDoc = """{"m":{"type":"com.github.jershell.kbson.models.polymorph.IntMessage","number":42}}"""

        val polyJson = Json { serializersModule = pModule }
        val res1 = polyJson.encodeToString(MessageWrapper.serializer(), MessageWrapper(m = StringMessage("FortyTwo")))
        val res2 = polyJson.encodeToString(MessageWrapper.serializer(), MessageWrapper(m = IntMessage(42)))

        assertEquals(res1, mDoc)
        assertEquals(res2, nDoc)
    }

    @Test
    fun parsePolymorphism() {
        val pModule = SerializersModule {
            polymorphic(Message::class) {
                subclass(StringMessage::class, StringMessage.serializer())
                subclass(IntMessage::class, IntMessage.serializer())
            }
        }

        val mDoc = MessageWrapper(m = StringMessage("FortyTwo"))
        val nDoc = MessageWrapper(m = IntMessage(42))

        val polyJson = Json { serializersModule = pModule }
        val res1 = polyJson.decodeFromString(MessageWrapper.serializer(), """{"m":{"type":"com.github.jershell.kbson.models.polymorph.StringMessage","msg":"FortyTwo"}}""")
        val res2 = polyJson.decodeFromString(MessageWrapper.serializer(), """{"m":{"type":"com.github.jershell.kbson.models.polymorph.IntMessage","number":42}}""")

        assertEquals(res1, mDoc)
        assertEquals(res2, nDoc)
    }

    @Test
    fun parseArrayLikeSet() {
        val source = """{"littleSet": ["one", "two", "three"]}"""
        val result = Json.decodeFromString(WrapperSet.serializer(), source)
        assertEquals(WrapperSet(setOf("one", "two", "three")), result)
    }
}
