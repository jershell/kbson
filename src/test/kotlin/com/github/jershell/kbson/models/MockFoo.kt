package com.github.jershell.kbson.models

import kotlinx.serialization.Serializable

@Serializable
class MockFoo (
        val listA : List<List<String>>
)