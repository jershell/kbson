package com.github.jershell.kbson.models

import com.github.jershell.kbson.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Complex(
        @Serializable(with = DateSerializer::class)
        val createAt: Date,
        val pair: Pair<String, String>,
        val list: List<String>,
        val triple: Triple<String, String, String>,
        val map: Map<String, Int>,
        val booleanMap: Map<Boolean, String>,
        val doubleFloatMap: Map<Double, Float>
)