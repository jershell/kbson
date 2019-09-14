package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class NestedMap (
        val name: String,
        val map: Map<String, Value>
)