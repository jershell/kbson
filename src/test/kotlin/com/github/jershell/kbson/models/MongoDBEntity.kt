package com.github.jershell.kbson.models

import com.github.jershell.kbson.NonEncodeNull

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.util.Date


@Serializable
open class MongoDBEntity(
        @NonEncodeNull
        @SerialName("_id")
        @Contextual
        val id: ObjectId? = null,

        @Contextual
        var createdAt: Date? = null,

        @Contextual
        var updatedAt: Date? = null
)
