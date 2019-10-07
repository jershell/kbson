package com.github.jershell.kbson.models.polymorph

import kotlinx.serialization.Serializable

sealed class SMessage {
    @Serializable
    class Error : SMessage()

    @Serializable
    class Loading : SMessage()

    @Serializable
    data class Data(val someData: String) : SMessage()
}