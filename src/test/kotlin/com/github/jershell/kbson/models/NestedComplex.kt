package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class NestedComplex(
        val mapOfMaps: Map<Byte, Map<Char, SimpleNG>>,
        val listOfMaps: List<Map<Int, List<Int>>>
)