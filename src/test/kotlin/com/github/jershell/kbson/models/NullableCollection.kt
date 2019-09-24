package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class NullableCollection (
        val fieldNull: String?,
        val list: List<String?>,
        val map: Map<String, Int?>
)