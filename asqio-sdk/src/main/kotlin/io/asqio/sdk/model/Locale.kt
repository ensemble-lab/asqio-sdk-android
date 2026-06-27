package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** メール文面の言語設定 */
@Serializable
public enum class UserLocale {
    @SerialName("ja")
    JA,

    @SerialName("en")
    EN,
}
