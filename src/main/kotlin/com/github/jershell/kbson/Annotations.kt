package com.github.jershell.kbson

import kotlinx.serialization.InheritableSerialInfo

/**
 * Specifies key for class discriminator value used during polymorphic serialization in [KBson].
 * Provided key is used only for an annotated class and its subclasses;
 * to configure global class discriminator, use [Configuration.classDiscriminator]
 * property.
 *
 * This annotation is [inheritable][InheritableSerialInfo], so it should be sufficient to place it on a base class of hierarchy.
 * It is not possible to define different class discriminators for different parts of class hierarchy.
 * Pay attention to the fact that class discriminator, same as polymorphic serializer's base class, is
 * determined statically.
 *
 * Example:
 * ```
 * @Serializable
 * @BsonClassDiscriminator("isoCountryCode") // sets discriminator for inheritance hierarchy
 * sealed interface ImportDocument
 *
 * @Serializable
 * @SerialName("JP") // will result in the element "isoCountryCode": "JP"
 * class JapaneseImportDocument(
 *     val id: Long,
 * ) : ImportDocument
 * ```
 *
 * @see Configuration.classDiscriminator
 */
@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
annotation class BsonClassDiscriminator(val discriminator: String)
