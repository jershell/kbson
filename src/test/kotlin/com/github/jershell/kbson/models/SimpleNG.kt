package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable


@Serializable
data class SimpleNG(
        val short: Short,
        val list: List<Char>
)