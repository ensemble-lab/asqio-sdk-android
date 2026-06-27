package io.asqio.sdk.service

import io.asqio.sdk.model.Message
import io.asqio.sdk.model.PaginationMeta
import io.asqio.sdk.network.ApiClient
import io.asqio.sdk.network.ApiEndpoint
import io.asqio.sdk.network.MessageListResponse

/** メッセージ一覧 + ページネーション情報 */
public data class MessageListResult(
    val messages: List<Message>,
    val meta: PaginationMeta,
)

/** メッセージ操作（メッセージ一覧／投稿） */
public class MessageService internal constructor(
    private val client: ApiClient,
) {
    /**
     * チケット内のメッセージ一覧を取得します。
     *
     * @param ticketId 対象チケットの ID
     * @param page 1 始まりのページ番号
     * @param perPage 1 ページあたりの件数
     */
    public suspend fun listMessages(
        ticketId: String,
        page: Int = 1,
        perPage: Int = 50,
    ): MessageListResult {
        val response = client.request(
            ApiEndpoint.ListMessages(ticketId, page, perPage),
            MessageListResponse.serializer(),
        )
        return MessageListResult(response.messages, response.meta)
    }

    /**
     * メッセージを投稿します。
     */
    public suspend fun postMessage(ticketId: String, body: String): Message {
        return client.request(
            ApiEndpoint.PostMessage(ticketId, body),
            Message.serializer(),
        )
    }
}
