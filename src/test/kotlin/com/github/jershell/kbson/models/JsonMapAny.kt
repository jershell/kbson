package com.github.jershell.kbson.models

import com.github.jershell.kbson.MapAnySerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class JsonMapAny(
        val title: String,
        @Serializable(with = MapAnySerializer::class)
        val payload: Map<String, @Contextual Any?>
)
