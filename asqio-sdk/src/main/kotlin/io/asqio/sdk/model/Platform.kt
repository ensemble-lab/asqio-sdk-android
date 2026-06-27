package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SDK プラットフォーム種別 */
@Serializable
public enum class Platform {
    @SerialName("ios")
    IOS,

    @SerialName("android")
    ANDROID,

    @SerialName("web")
    WEB,
}
