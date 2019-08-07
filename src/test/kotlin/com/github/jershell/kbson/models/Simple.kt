package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class Simple (
        val valueString: String,
        val valueDouble: Double,
        val valueFloat: Float,
        val valueLong: Long,
        val valueChar: Char,
        val valueBool: Boolean,
        val valueInt: Int
)