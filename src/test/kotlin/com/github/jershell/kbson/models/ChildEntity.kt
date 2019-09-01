package com.github.jershell.kbson.models

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ChildEntity(
        val userId: String
) : MongoDBEntity()