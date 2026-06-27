package io.asqio.sdk.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** スレッド内の 1 メッセージ */
@Serializable
public data class Message(
    val id: String,
    @SerialName("sender_type") val senderType: SenderType,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: Instant,
)
