package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class SealedWrapper(@Polymorphic val payload: SMessage)