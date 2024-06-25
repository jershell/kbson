![Maven Central](https://img.shields.io/maven-central/v/com.github.jershell/kbson)  ![Bintray](https://img.shields.io/bintray/v/jershell/generic/kbson?label=jcenter)

# kbson

# What is this?
This adapter adds BSON support to kotlinx.serialization.

BSON types supported:

- Double	 
- String 
- Array
- Binary 
- UUID
- ObjectId
- Boolean
- Date
- Symbol
- 32-bit integer
- Long
- Decimal128


## Install

build.gradle.kts
```kotlin
dependencies {
    // kotlin 1.7.20 & kotlinx.serialization 1.4.1
    implementation("com.github.jershell:kbson:0.5.1")
    
    // kotlin 1.6.21 & kotlinx.serialization 1.3.3
    implementation("com.github.jershell:kbson:0.4.6")


    // kotlin 1.4.20 & kotlinx.serialization 1.0.1
    implementation("com.github.jershell:kbson:0.4.4")
    
    // kotlin 1.3
    implementation("com.github.jershell:kbson:0.3.0")
}
```

or

build.gradle
```groovy
dependencies {
    // kotlin 1.7.20 & kotlinx.serialization 1.4.1
    implementation 'com.github.jershell:kbson:0.5.0'
    
    // kotlin 1.6.21 & kotlinx.serialization 1.3.3
    implementation 'com.github.jershell:kbson:0.4.6'
    
    
    // kotlin 1.4.20 & kotlinx.serialization 1.0.1
    implementation 'com.github.jershell:kbson:0.4.4'
    
    
    // kotlin 1.3
    implementation 'com.github.jershell:kbson:0.3.0'
}
```


# Usage samples
##### Parsing from BSON to a Kotlin object

```kotlin
import kotlinx.serialization.Serializable
import com.github.jershell.kbson.ObjectIdSerializer

val kBson = KBson()


@Serializable
data class Simple (
        val valueString: String,

        val valueDouble: Double,

        val valueFloat: Float,

        val valueLong: Long,

        val valueChar: Char,

        val valueBool: Boolean,

        @NonEncodeNull
        @ContextualSerialization
        val _id: ObjectId? = null,

        @ContextualSerialization
        val uuid: UUID,

        val valueInt: Int
)

val simple = kBson.parse(Simple.serializer(), bsonDocFromMongoJavaDriver)
```

##### Serializing from a Kotlin object to BSON
```kotlin
import kotlinx.serialization.Serializable
import com.github.jershell.kbson.Configuration
import com.github.jershell.kbson.KBson
import com.github.jershell.kbson.ObjectIdSerializer

@Serializable
data class Simple (
        val valueString: String = "default",
        val valueDouble: Double,
        val valueFloat: Float,
        val valueLong: Long,
        val valueChar: Char,
        val valueBool: Boolean,
        @NonEncodeNull
        @ContextualSerialization 
        val _id: ObjectId? = null,
        val valueInt: Int
)

// Optional configuration
val kBson = KBson(Configuration(encodeDefaults = false))
val bsonDoc = kBson.stringify(Simple.serializer(), simpleModel)

// You can override default serializers or add your serializer  
val kBson = KBson(context = serializersModuleOf(mapOf(
            ObjectId::class to ObjectIdSerializer,
            Date::class to DateSerializer
    )))
    
// You can use load and dump with ByteArray 
val simple = kBson.load(Simple.serializer(), bsonDoc)
// also
val simple2 = kBson.load(Simple.serializer(), bsonDoc.toByteArray())
// !!!! The method load() use BsonDecoder and strict order of fields

```
[See the tests for more examples](https://github.com/jershell/kbson/blob/master/src/test/kotlin/com/github/jershell/kbson/KBsonTest.kt) 
## API
```kotlin
val kBson = KBson()

kBson.parse(deserializer: DeserializationStrategy<T>, doc: BsonDocument) :T
// !!!! The method load() use BsonDecoder and strict order of fields
// https://docs.mongodb.com/manual/tutorial/update-documents/#field-order
kBson.load(deserializer: DeserializationStrategy<T>, doc: ByteArray): T
kBson.load(deserializer: DeserializationStrategy<T>, doc: BsonDocument): T

kBson.stringify(): BsonDocument
kBson.dump(serializer: SerializationStrategy<T>, obj: T): ByteArray

// Extension functions 
BsonDocument.toByteArray(): ByteArray
BsonDocument.toDocument(): Document
```

### Configuration
```
classDiscriminator = "___type" // name of the class descriptor property in polymorphic serialization.
encodeDefaults = "true" // specifies whether default values are encoded.
```
###### ps
@NonEncodeNull useful for item _id mongodb

The default enum class supported like string. You can also override it

kbson before 0.1.5 use kotlinx.serialization 0.11.x

kbson after 0.1.5 use kotlinx.serialization 0.13.x

kbson after 0.2.0 use kotlinx.serialization 0.14.x

kbson after 0.2.2 use kotlinx.serialization 0.20.0 and kotlin 1.3.71

kbson after 0.4.0 use kotlinx.serialization "1.0-M1-1.4.0-rc" and kotlin 1.4.0-rc

kbson after 0.4.1 use kotlinx.serialization "1.0.0" and kotlin 1.4.10

kbson after 0.4.3 use kotlinx.serialization "1.0.0" and kotlin 1.4.20

kbson after 0.4.5 use kotlinx.serialization "1.3.3" and kotlin 1.6.21

kbson after 0.4.6 use kotlinx.serialization "1.3.3" and kotlin 1.6.21

kbson after 0.5.0 use kotlinx.serialization "1.4.1" and kotlin 1.7.20


# Contributing to kbson
Pull requests and bug reports are always welcome!

To run the tests: ./gradlew check

# Reference links
- http://litote.org/kmongo/ | https://github.com/Litote/kmongo
- https://github.com/Kotlin/kotlinx.serialization
- https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
- https://mongodb.github.io/mongo-java-driver/
- Special thanks to: https://github.com/charleskorn/kaml
