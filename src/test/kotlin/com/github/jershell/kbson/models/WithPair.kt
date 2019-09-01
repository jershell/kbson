package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class WithPair (
        val pair: Pair<String?, String?>,
        val triple: Triple<Int, String?, Long>
)