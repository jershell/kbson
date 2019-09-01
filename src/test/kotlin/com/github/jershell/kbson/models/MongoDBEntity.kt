package com.github.jershell.kbson.models

import com.github.jershell.kbson.NonEncodeNull

import com.github.jershell.kbson.ObjectIdSerializer
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.util.Date


@Serializable
open class MongoDBEntity(
        @NonEncodeNull
        @SerialName("_id")
        @ContextualSerialization
        val id: ObjectId? = null,

        @ContextualSerialization
        var createdAt: Date? = null,

        @ContextualSerialization
        var updatedAt: Date? = null
)
