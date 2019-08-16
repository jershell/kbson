package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class OptionalClass(
        val reqString: String = "default_value1",
        val reqString3: String = "default_value2"
)