package com.github.jershell.kbson.models

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class WrapperMapWithObjectId(
        val elements: Map<@ContextualSerialization ObjectId, String>
)