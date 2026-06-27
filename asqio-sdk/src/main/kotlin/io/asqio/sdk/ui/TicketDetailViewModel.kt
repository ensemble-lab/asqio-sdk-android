package io.asqio.sdk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.asqio.sdk.error.AsqioError
import io.asqio.sdk.model.Message
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.service.MessageService
import io.asqio.sdk.service.TicketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class TicketDetailUiState(
    val ticket: Ticket,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val hasMore: Boolean = false,
    val error: AsqioError? = null,
)

internal class TicketDetailViewModel(
    private val ticket: Ticket,
    private val ticketService: TicketService,
    private val messageService: MessageService,
    private val onMarkedAsRead: (String) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TicketDetailUiState(
            ticket = ticket,
            messages = ticket.messages.orEmpty(),
        )
    )
    val state: StateFlow<TicketDetailUiState> = _state.asStateFlow()

    private var currentPage = 1
    private val perPage = 50

    init {
        loadMessages()
        markAsRead()
    }

    fun loadMessages() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = messageService.listMessages(ticket.id, page = 1, perPage = perPage)
                currentPage = 1
                _state.update {
                    it.copy(
                        messages = result.messages.reversed(),
                        hasMore = result.meta.currentPage < result.meta.totalPages,
                        isLoading = false,
                    )
                }
            } catch (e: AsqioError) {
                _state.update { it.copy(error = e, isLoading = false) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val result = messageService.listMessages(ticket.id, page = nextPage, perPage = perPage)
                currentPage = nextPage
                _state.update {
                    it.copy(
                        messages = result.messages.reversed() + it.messages,
                        hasMore = result.meta.currentPage < result.meta.totalPages,
                        isLoadingMore = false,
                    )
                }
            } catch (_: AsqioError) {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun sendMessage(body: String) {
        if (_state.value.isSending) return
        _state.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                val message = messageService.postMessage(ticket.id, body)
                _state.update {
                    it.copy(messages = it.messages + message, isSending = false)
                }
            } catch (e: AsqioError) {
                _state.update { it.copy(error = e, isSending = false) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                ticketService.markAsRead(ticket.id)
                onMarkedAsRead(ticket.id)
            } catch (_: AsqioError) {
                // 既読化エラーは UI で表面化させない
            }
        }
    }

    class Factory(
        private val ticket: Ticket,
        private val ticketService: TicketService,
        private val messageService: MessageService,
        private val onMarkedAsRead: (String) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketDetailViewModel(ticket, ticketService, messageService, onMarkedAsRead) as T
        }
    }
}
