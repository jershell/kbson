package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

@Serializable
data class FooTimestampedMessage(override val timestamp: Int): TimestampedMessage()