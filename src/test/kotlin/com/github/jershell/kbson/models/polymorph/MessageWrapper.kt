package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

@Serializable
data class MessageWrapper(val m: Message)