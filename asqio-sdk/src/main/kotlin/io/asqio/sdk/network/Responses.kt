package io.asqio.sdk.network

import io.asqio.sdk.model.Message
import io.asqio.sdk.model.PaginationMeta
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.model.Topic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TopicListResponse(
    val topics: List<Topic>,
)

@Serializable
internal data class TicketListResponse(
    val tickets: List<Ticket>,
    val meta: PaginationMeta,
)

@Serializable
internal data class MessageListResponse(
    val messages: List<Message>,
    val meta: PaginationMeta,
)

@Serializable
internal data class UnreadCountResponse(
    @SerialName("unread_count") val unreadCount: Int,
)
