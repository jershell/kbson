package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

sealed class SMessage {
    @Serializable
    object Error : SMessage()

    @Serializable
    object Loading : SMessage()

    @Serializable
    data class Data(val someData: String) : SMessage()

    @Serializable
    data class DataWithId(val someData: String, val _id: String) : SMessage()

    @Serializable
    data class DataWithObjectId(val someData: String, @Contextual val _id: ObjectId) : SMessage()
}