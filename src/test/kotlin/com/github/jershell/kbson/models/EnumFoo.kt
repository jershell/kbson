package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

enum class SEX {
    MALE, FEMALE, TRANSGENDER
}

enum class SexWithValue(val sign: Char) {
    MALE('♂'),
    FEMALE('♀'),
    TRANSGENDER('⚦')
}

@Serializable
data class EnumFoo(
        val sex: SEX = SEX.FEMALE,
        val sex2: SexWithValue = SexWithValue.MALE
)