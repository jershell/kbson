package com.github.jershell.kbson.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MongoItem(
//        @Serializable(with = ObjectIdSerializer::class)
        @Contextual
        val _id: ObjectId? = null
)