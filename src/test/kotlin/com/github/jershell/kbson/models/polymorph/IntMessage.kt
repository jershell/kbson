package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

@Serializable
data class IntMessage(val number: Int): Message