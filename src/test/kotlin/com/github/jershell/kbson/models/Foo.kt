package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class Foo(
        val key_A: Map<String, String>,
        val key_B: Map<String, String>
)