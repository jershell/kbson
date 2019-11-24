package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
data class NullableNotOptionalClass(val s: String, val nullable: String?, val test: String = "a")