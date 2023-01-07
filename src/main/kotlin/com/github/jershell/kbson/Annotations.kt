package com.github.jershell.kbson

import kotlinx.serialization.InheritableSerialInfo


@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
annotation class BsonClassDiscriminator(val discriminator: String)
