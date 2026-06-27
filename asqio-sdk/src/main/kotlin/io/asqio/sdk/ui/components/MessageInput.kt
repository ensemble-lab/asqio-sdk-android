package io.asqio.sdk.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * メッセージ入力フォーム。
 *
 * @param onSend 送信ボタン or Enter キー押下時のコールバック
 * @param isSending true の間は入力／送信ボタンを無効化
 * @param placeholder 入力欄のプレースホルダ
 */
@Composable
public fun MessageInput(
    onSend: (String) -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "メッセージを入力...",
) {
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = remember(text, isSending) { text.trim().isNotEmpty() && !isSending }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = { Text(placeholder) },
            enabled = !isSending,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )
        Button(
            onClick = {
                val trimmed = text.trim()
                if (trimmed.isNotEmpty()) {
                    onSend(trimmed)
                    text = ""
                }
            },
            enabled = canSend,
        ) {
            Text(if (isSending) "送信中..." else "送信")
        }
    }
}
