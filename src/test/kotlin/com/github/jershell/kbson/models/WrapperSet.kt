package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class WrapperSet(
        val littleSet : Set<String> = setOf("aa", "bb", "cc")
)