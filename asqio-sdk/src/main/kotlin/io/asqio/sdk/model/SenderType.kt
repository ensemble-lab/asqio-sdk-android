package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** メッセージ送信者の種別 */
@Serializable
public enum class SenderType {
    @SerialName("user")
    USER,

    @SerialName("operator")
    OPERATOR,
}
