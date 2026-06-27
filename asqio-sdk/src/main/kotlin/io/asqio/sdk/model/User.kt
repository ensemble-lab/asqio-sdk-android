package io.asqio.sdk.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SDK 側 (エンドユーザー) のプロファイル */
@Serializable
public data class User(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    val name: String? = null,
    val locale: UserLocale? = null,
    @SerialName("updated_at") val updatedAt: Instant,
)
