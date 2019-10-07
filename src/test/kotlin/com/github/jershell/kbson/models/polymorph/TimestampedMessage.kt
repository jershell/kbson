package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

@Serializable
abstract class TimestampedMessage : Message {
    abstract val timestamp: Int
}

