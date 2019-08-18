package com.github.jershell.kbson.models

import com.github.jershell.kbson.*
import kotlinx.serialization.*
import org.bson.types.ObjectId
import java.math.BigDecimal

@Serializable
data class Custom (
        @Serializable(with = ObjectIdSerializer::class)
        @SerialName("_id")
        @NonEncodeNull
        val id: ObjectId? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val dec128: BigDecimal
)