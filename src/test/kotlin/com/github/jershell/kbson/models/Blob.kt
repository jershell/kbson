package com.github.jershell.kbson.models

import com.github.jershell.kbson.ByteArraySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
class Blob (
        @Serializable(ByteArraySerializer::class) val img: ByteArray,
        @Serializable(ByteArraySerializer::class) val txt: ByteArray,
        @Serializable(ByteArraySerializer::class) val zipFile: ByteArray
)