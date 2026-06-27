package io.asqio.sdk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.asqio.sdk.error.AsqioError
import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.model.Topic
import io.asqio.sdk.service.TicketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class NewTicketUiState(
    val title: String = "",
    val message: String = "",
    val selectedTopicId: String? = null,
    val topics: List<Topic> = emptyList(),
    val isSubmitting: Boolean = false,
    val error: AsqioError? = null,
) {
    val canSubmit: Boolean get() = message.trim().isNotEmpty() && !isSubmitting
}

internal class NewTicketViewModel(
    private val ticketService: TicketService,
    private val deviceInfo: DeviceInfo,
    private val context: Map<String, String>?,
) : ViewModel() {

    private val _state = MutableStateFlow(NewTicketUiState())
    val state: StateFlow<NewTicketUiState> = _state.asStateFlow()

    init {
        loadTopics()
    }

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value) }
    }

    fun updateMessage(value: String) {
        _state.update { it.copy(message = value) }
    }

    fun selectTopic(topicId: String?) {
        _state.update { it.copy(selectedTopicId = topicId) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            try {
                val topics = ticketService.listTopics()
                _state.update { it.copy(topics = topics) }
            } catch (_: AsqioError) {
                // トピック取得失敗時はトピック選択 UI を出さないだけで続行可能
            }
        }
    }

    /** チケットを作成。成功時は Ticket、失敗時は null を返す。 */
    suspend fun submit(): Ticket? {
        val current = _state.value
        val trimmedMessage = current.message.trim()
        if (trimmedMessage.isEmpty()) return null

        _state.update { it.copy(isSubmitting = true, error = null) }
        return try {
            val titleToSend = current.title.trim().ifEmpty { null }
            val ticket = ticketService.createTicket(
                message = trimmedMessage,
                title = titleToSend,
                topicId = current.selectedTopicId,
                context = context,
                deviceInfo = deviceInfo,
            )
            _state.update { it.copy(isSubmitting = false) }
            ticket
        } catch (e: AsqioError) {
            _state.update { it.copy(isSubmitting = false, error = e) }
            null
        }
    }

    class Factory(
        private val ticketService: TicketService,
        private val deviceInfo: DeviceInfo,
        private val context: Map<String, String>?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewTicketViewModel(ticketService, deviceInfo, context) as T
        }
    }
}
