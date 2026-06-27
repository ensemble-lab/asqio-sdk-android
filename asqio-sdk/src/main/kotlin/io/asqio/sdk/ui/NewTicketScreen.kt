package io.asqio.sdk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.Ticket
import io.asqio.sdk.service.TicketService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewTicketScreen(
    ticketService: TicketService,
    deviceInfo: DeviceInfo,
    context: Map<String, String>?,
    onCreated: (Ticket) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: NewTicketViewModel = viewModel(
        factory = NewTicketViewModel.Factory(ticketService, deviceInfo, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("新規お問い合わせ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val ticket = viewModel.submit()
                                if (ticket != null) onCreated(ticket)
                            }
                        },
                        enabled = state.canSubmit,
                    ) {
                        Text("送信")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.topics.isNotEmpty()) {
                    TopicDropdown(
                        topics = state.topics.map { it.id to it.name },
                        selectedId = state.selectedTopicId,
                        onSelect = { viewModel.selectTopic(it) },
                        enabled = !state.isSubmitting,
                    )
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("件名（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                )

                OutlinedTextField(
                    value = state.message,
                    onValueChange = viewModel::updateMessage,
                    label = { Text("お問い合わせ内容") },
                    placeholder = { Text("できるだけ詳しくお書きください") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    enabled = !state.isSubmitting,
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val ticket = viewModel.submit()
                            if (ticket != null) onCreated(ticket)
                        }
                    },
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSubmitting) "送信中..." else "送信")
                }
            }

            if (state.isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            },
            title = { Text("エラー") },
            text = { Text(error.message ?: "予期しないエラーが発生しました") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDropdown(
    topics: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    enabled: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedName = remember(selectedId, topics) {
        topics.firstOrNull { it.first == selectedId }?.second ?: "選択しない"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("トピック") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            enabled = enabled,
        )
        TopicDropdownMenu(
            scope = this,
            expanded = expanded,
            topics = topics,
            onSelect = { selected ->
                onSelect(selected)
                expanded = false
            },
            onDismiss = { expanded = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDropdownMenu(
    scope: ExposedDropdownMenuBoxScope,
    expanded: Boolean,
    topics: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    with(scope) {
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
        ) {
            DropdownMenuItem(
                text = { Text("選択しない") },
                onClick = { onSelect(null) },
            )
            topics.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id) },
                )
            }
        }
    }
}
