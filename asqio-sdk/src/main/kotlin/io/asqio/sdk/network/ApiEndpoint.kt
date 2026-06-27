package io.asqio.sdk.network

import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.TokenType
import io.asqio.sdk.model.UserLocale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

internal enum class HttpMethod { GET, POST, PUT, DELETE }

/** API エンドポイントを記述する内部 sealed class */
internal sealed class ApiEndpoint {
    abstract val method: HttpMethod
    abstract val path: String
    open val query: List<Pair<String, String>> = emptyList()
    open val body: JsonObject? = null

    // --- Topics ---

    object ListTopics : ApiEndpoint() {
        override val method = HttpMethod.GET
        override val path = "/api/v1/topics"
    }

    // --- Tickets ---

    data class ListTickets(val page: Int, val perPage: Int) : ApiEndpoint() {
        override val method = HttpMethod.GET
        override val path = "/api/v1/tickets"
        override val query = listOf("page" to page.toString(), "per_page" to perPage.toString())
    }

    data class GetTicket(val id: String) : ApiEndpoint() {
        override val method = HttpMethod.GET
        override val path = "/api/v1/tickets/$id"
    }

    data class CreateTicket(
        val message: String,
        val title: String?,
        val topicId: String?,
        val context: Map<String, String>?,
        val deviceInfo: DeviceInfo,
    ) : ApiEndpoint() {
        override val method = HttpMethod.POST
        override val path = "/api/v1/tickets"
        override val body: JsonObject = buildJsonObject {
            put("message", JsonPrimitive(message))
            title?.let { put("title", JsonPrimitive(it)) }
            topicId?.let { put("topic_id", JsonPrimitive(it)) }
            context?.let { ctx ->
                putJsonObject("context") {
                    ctx.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                }
            }
            deviceInfo.toMap().forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        }
    }

    data class MarkAsRead(val ticketId: String) : ApiEndpoint() {
        override val method = HttpMethod.POST
        override val path = "/api/v1/tickets/$ticketId/read"
    }

    object UnreadCount : ApiEndpoint() {
        override val method = HttpMethod.GET
        override val path = "/api/v1/unread_count"
    }

    // --- Messages ---

    data class ListMessages(
        val ticketId: String,
        val page: Int,
        val perPage: Int,
    ) : ApiEndpoint() {
        override val method = HttpMethod.GET
        override val path = "/api/v1/tickets/$ticketId/messages"
        override val query = listOf("page" to page.toString(), "per_page" to perPage.toString())
    }

    data class PostMessage(val ticketId: String, val messageBody: String) : ApiEndpoint() {
        override val method = HttpMethod.POST
        override val path = "/api/v1/tickets/$ticketId/messages"
        override val body: JsonObject = buildJsonObject {
            putJsonObject("message") {
                put("body", JsonPrimitive(messageBody))
            }
        }
    }

    // --- User (identify) ---

    data class UpdateMe(
        val email: String?,
        val name: String?,
        val locale: UserLocale?,
    ) : ApiEndpoint() {
        override val method = HttpMethod.PUT
        override val path = "/api/v1/me"
        override val body: JsonObject = buildJsonObject {
            email?.let { put("email", JsonPrimitive(it)) }
            name?.let { put("name", JsonPrimitive(it)) }
            locale?.let {
                put(
                    "locale",
                    JsonPrimitive(
                        when (it) {
                            UserLocale.JA -> "ja"
                            UserLocale.EN -> "en"
                        }
                    )
                )
            }
        }
    }

    // --- Devices ---

    data class RegisterDevice(
        val pushToken: String,
        val tokenType: TokenType,
        val deviceInfo: DeviceInfo,
    ) : ApiEndpoint() {
        override val method = HttpMethod.POST
        override val path = "/api/v1/devices"
        override val body: JsonObject = buildJsonObject {
            putJsonObject("device") {
                put("platform", JsonPrimitive("android"))
                put("push_token", JsonPrimitive(pushToken))
                put(
                    "token_type",
                    JsonPrimitive(
                        when (tokenType) {
                            TokenType.APNS -> "apns"
                            TokenType.FCM -> "fcm"
                        }
                    )
                )
                deviceInfo.toMapWithoutPlatform().forEach { (k, v) ->
                    put(k, JsonPrimitive(v))
                }
            }
        }
    }

    data class UpdateDevice(
        val id: String,
        val pushToken: String?,
        val osVersion: String?,
        val appVersion: String?,
    ) : ApiEndpoint() {
        override val method = HttpMethod.PUT
        override val path = "/api/v1/devices/$id"
        override val body: JsonObject = buildJsonObject {
            putJsonObject("device") {
                pushToken?.let { put("push_token", JsonPrimitive(it)) }
                osVersion?.let { put("os_version", JsonPrimitive(it)) }
                appVersion?.let { put("app_version", JsonPrimitive(it)) }
            }
        }
    }

    data class DeleteDevice(val id: String) : ApiEndpoint() {
        override val method = HttpMethod.DELETE
        override val path = "/api/v1/devices/$id"
    }
}
