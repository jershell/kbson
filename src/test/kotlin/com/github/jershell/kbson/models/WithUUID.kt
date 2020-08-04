package com.github.jershell.kbson.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WithUUID (
        @Contextual
        val uuid : UUID,
        val map: Map<String,  @Contextual UUID>
)