package io.asqio.sdk.model

import kotlinx.serialization.Serializable

/** プリセットトピック */
@Serializable
public data class Topic(
    val id: String,
    val name: String,
)
