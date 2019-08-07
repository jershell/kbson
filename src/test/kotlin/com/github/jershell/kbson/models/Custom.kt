package com.github.jershell.kbson.models

import com.github.jershell.kbson.BigDecimalSerializer
import com.github.jershell.kbson.ObjectIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.math.BigDecimal

@Serializable
data class Custom (
        @Serializable(with = ObjectIdSerializer::class)
        @SerialName("_id")
        val id: ObjectId,

        @Serializable(with = BigDecimalSerializer::class) val dec128: BigDecimal

)