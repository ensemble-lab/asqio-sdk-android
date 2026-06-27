package io.asqio.sdk.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 問い合わせチケット（スレッド） */
@Serializable
public data class Ticket(
    val id: String,
    val title: String? = null,
    val topic: Topic? = null,
    val context: Map<String, String>? = null,
    @SerialName("device_info") val deviceInfo: TicketDeviceInfo? = null,
    val unread: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
    val messages: List<Message>? = null,
)

/** チケット作成時にサーバへ保存される端末情報のスナップショット */
@Serializable
public data class TicketDeviceInfo(
    val platform: String? = null,
    @SerialName("os_version") val osVersion: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
)
