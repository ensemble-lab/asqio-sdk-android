package io.asqio.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API レスポンスのページネーション情報 */
@Serializable
public data class PaginationMeta(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("per_page") val perPage: Int,
)
