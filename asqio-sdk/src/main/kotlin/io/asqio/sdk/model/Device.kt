package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 登録済みデバイス情報 */
@Serializable
public data class Device(
    val id: String,
    val platform: Platform,
    @SerialName("push_token") val pushToken: String? = null,
    @SerialName("os_version") val osVersion: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
)
