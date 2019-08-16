package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class NullableClass (
        val str: String?
)