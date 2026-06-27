package io.asqio.sdk.service

import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.PaginationMeta
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.model.Topic
import io.asqio.sdk.network.ApiClient
import io.asqio.sdk.network.ApiEndpoint
import io.asqio.sdk.network.TicketListResponse
import io.asqio.sdk.network.TopicListResponse
import io.asqio.sdk.network.UnreadCountResponse

/** チケット一覧 + ページネーション情報 */
public data class TicketListResult(
    val tickets: List<Ticket>,
    val meta: PaginationMeta,
)

/** チケット操作（スレッド一覧／作成／既読／未読数） */
public class TicketService internal constructor(
    private val client: ApiClient,
) {
    /**
     * チケット一覧を取得します。
     *
     * @param page 1 始まりのページ番号
     * @param perPage 1 ページあたりの件数
     */
    public suspend fun listTickets(page: Int = 1, perPage: Int = 20): TicketListResult {
        val response = client.request(
            ApiEndpoint.ListTickets(page, perPage),
            TicketListResponse.serializer(),
        )
        return TicketListResult(response.tickets, response.meta)
    }

    /**
     * 新規チケットを作成します。
     *
     * @param message 最初のメッセージ本文（必須）
     * @param title チケット件名（省略時はサーバ側で初回メッセージから自動生成）
     * @param topicId プリセットトピックの ID
     * @param context チケットに付与するコンテキスト（key-value）。運営者画面に表示
     * @param deviceInfo 端末情報（呼び出し側で [DeviceInfo.current] を渡すことを想定）
     */
    public suspend fun createTicket(
        message: String,
        title: String? = null,
        topicId: String? = null,
        context: Map<String, String>? = null,
        deviceInfo: DeviceInfo,
    ): Ticket {
        return client.request(
            ApiEndpoint.CreateTicket(
                message = message,
                title = title,
                topicId = topicId,
                context = context,
                deviceInfo = deviceInfo,
            ),
            Ticket.serializer(),
        )
    }

    /** トピック一覧を取得します。 */
    public suspend fun listTopics(): List<Topic> {
        val response = client.request(
            ApiEndpoint.ListTopics,
            TopicListResponse.serializer(),
        )
        return response.topics
    }

    /** チケット詳細を取得します（メッセージ含む） */
    public suspend fun getTicket(id: String): Ticket {
        return client.request(ApiEndpoint.GetTicket(id), Ticket.serializer())
    }

    /** チケットを既読にします */
    public suspend fun markAsRead(ticketId: String) {
        client.requestVoid(ApiEndpoint.MarkAsRead(ticketId))
    }

    /** 未読チケット数を取得します */
    public suspend fun getUnreadCount(): Int {
        val response = client.request(
            ApiEndpoint.UnreadCount,
            UnreadCountResponse.serializer(),
        )
        return response.unreadCount
    }
}
