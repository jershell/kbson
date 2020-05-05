package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class KeyByEnum (
        val map: Map<SEX, Int>
)