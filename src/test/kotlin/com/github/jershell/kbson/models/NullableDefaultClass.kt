package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class NullableDefaultClass (
        val list: List<String>? = null
)