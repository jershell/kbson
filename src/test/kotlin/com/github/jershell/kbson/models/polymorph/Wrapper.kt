package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

@Serializable
data class Wrapper(
        val request: Message,
        val response: TimestampedMessage
)