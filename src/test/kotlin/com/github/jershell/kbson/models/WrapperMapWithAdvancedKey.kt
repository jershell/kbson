package com.github.jershell.kbson.models

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.util.*

@Serializable
data class WrapperMapWithAdvancedKey(
        val itemsDate: Map<@ContextualSerialization Date, String>,
        val itemsBigDecimal: Map<@ContextualSerialization BigDecimal, String>
)