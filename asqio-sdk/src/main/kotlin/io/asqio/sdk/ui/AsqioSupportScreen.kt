package io.asqio.sdk.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.service.MessageService
import io.asqio.sdk.service.TicketService

internal sealed class AsqioView {
    data object List : AsqioView()
    data class Detail(val ticket: Ticket) : AsqioView()
    data object NewTicket : AsqioView()
}

/**
 * 内部実装。 外部からは [io.asqio.sdk.AsqioSupport.AsqioSupportScreen] を呼ぶことを想定。
 *
 * スレッド一覧 / 詳細 / 新規作成の 3 ビューを単一の Composable 内で切替えます。
 */
@Composable
internal fun AsqioSupportInternalScreen(
    ticketService: TicketService,
    messageService: MessageService,
    deviceInfo: DeviceInfo,
    context: Map<String, String>?,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable(stateSaver = AsqioViewSaver) {
        mutableStateOf<AsqioView>(AsqioView.List)
    }

    // リスト VM をこのレベルで保持することで、Detail / NewTicket の操作結果
    // （新規作成 / 既読化）を一覧へ即時反映する。
    val listViewModel: TicketListViewModel = viewModel(
        factory = TicketListViewModel.Factory(ticketService),
    )

    when (val current = view) {
        AsqioView.List -> TicketListScreen(
            viewModel = listViewModel,
            onTicketSelected = { view = AsqioView.Detail(it) },
            onNewTicket = { view = AsqioView.NewTicket },
            modifier = modifier.fillMaxSize(),
        )

        is AsqioView.Detail -> TicketDetailScreen(
            ticket = current.ticket,
            ticketService = ticketService,
            messageService = messageService,
            onMarkedAsRead = { ticketId -> listViewModel.markAsReadLocally(ticketId) },
            onBack = { view = AsqioView.List },
            modifier = modifier.fillMaxSize(),
        )

        AsqioView.NewTicket -> NewTicketScreen(
            ticketService = ticketService,
            deviceInfo = deviceInfo,
            context = context,
            onCreated = { newTicket ->
                listViewModel.prependTicket(newTicket)
                view = AsqioView.Detail(newTicket)
            },
            onBack = { view = AsqioView.List },
            modifier = modifier.fillMaxSize(),
        )
    }
}

private val AsqioViewSaver: Saver<AsqioView, Any> = Saver(
    save = { value ->
        when (value) {
            AsqioView.List -> "list"
            AsqioView.NewTicket -> "new"
            is AsqioView.Detail -> "detail"
        }
    },
    restore = {
        // 詳細画面の復元には Ticket 全体が必要だが Saveable には載せないため、
        // プロセス再生成時は安全に一覧へ戻す。
        AsqioView.List
    },
)
