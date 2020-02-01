package com.github.jershell.kbson.models

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WithUUID (
        @ContextualSerialization
        val uuid : UUID,
        val map: Map<String,  @ContextualSerialization UUID>
)