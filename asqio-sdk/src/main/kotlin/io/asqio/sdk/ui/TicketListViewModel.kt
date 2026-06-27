package io.asqio.sdk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.asqio.sdk.error.AsqioError
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.service.TicketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class TicketListUiState(
    val tickets: List<Ticket> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: AsqioError? = null,
)

internal class TicketListViewModel(
    private val ticketService: TicketService,
) : ViewModel() {

    private val _state = MutableStateFlow(TicketListUiState())
    val state: StateFlow<TicketListUiState> = _state.asStateFlow()

    private var currentPage = 1
    private val perPage = 20

    init {
        load()
    }

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = ticketService.listTickets(page = 1, perPage = perPage)
                currentPage = 1
                _state.update {
                    it.copy(
                        tickets = result.tickets,
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
                val result = ticketService.listTickets(page = nextPage, perPage = perPage)
                currentPage = nextPage
                _state.update {
                    it.copy(
                        tickets = it.tickets + result.tickets,
                        hasMore = result.meta.currentPage < result.meta.totalPages,
                        isLoadingMore = false,
                    )
                }
            } catch (_: AsqioError) {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun prependTicket(ticket: Ticket) {
        _state.update { it.copy(tickets = listOf(ticket) + it.tickets) }
    }

    fun markAsReadLocally(ticketId: String) {
        _state.update { state ->
            state.copy(
                tickets = state.tickets.map {
                    if (it.id == ticketId) it.copy(unread = false) else it
                },
            )
        }
    }

    class Factory(private val ticketService: TicketService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketListViewModel(ticketService) as T
        }
    }
}
