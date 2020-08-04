package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class ChildEntity(
        val userId: String
) : MongoDBEntity()