package com.github.jershell.kbson.models

import com.github.jershell.kbson.NonEncodeNull
import com.github.jershell.kbson.ObjectIdSerializer
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import org.bson.types.ObjectId

@Serializable
data class MongoItem(
//        @Serializable(with = ObjectIdSerializer::class)
        @ContextualSerialization
        val _id: ObjectId? = null
)