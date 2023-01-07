package com.github.jershell.kbson.models.polymorph

import com.github.jershell.kbson.BsonClassDiscriminator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@BsonClassDiscriminator("contentType")
sealed interface Note

@Serializable
@SerialName("ADDRESS")
data class AddressNote(
    val street: String,
): Note

@Serializable
@SerialName("PHONE")
data class PhoneNote(
    val phoneNumber: String,
): Note
