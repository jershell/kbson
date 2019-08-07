package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class Nested (
        val n: Simple,
        val m: Int
)