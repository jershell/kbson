package com.github.jershell.kbson.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.*

@Serializable
data class WrapperMapWithAdvancedKey(
        val itemsDate: Map<@Contextual Date, String>,
        val itemsBigDecimal: Map<@Contextual BigDecimal, String>
)