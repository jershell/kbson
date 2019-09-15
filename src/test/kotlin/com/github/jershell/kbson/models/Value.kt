package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class Value(
        val value1: String,
        val value2: List<String>,
        val relationship: Int? = null,
        val default: String = ""
)