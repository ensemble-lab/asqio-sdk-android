package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Push トークンの種別 */
@Serializable
public enum class TokenType {
    @SerialName("apns")
    APNS,

    @SerialName("fcm")
    FCM,
}
