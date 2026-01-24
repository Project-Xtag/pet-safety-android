package com.petsafety.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.petsafety.app.R

@Composable
fun SuccessStoryDialog(
    petName: String,
    onDismiss: () -> Unit,
    onSubmit: (storyText: String) -> Unit,
    onSkip: () -> Unit
) {
    var storyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_your_story)) },
        text = {
            Column {
                Text(stringResource(R.string.success_story_prompt, petName))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    label = { Text(stringResource(R.string.your_story)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text(stringResource(R.string.story_placeholder)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(storyText) },
                enabled = storyText.isNotBlank()
            ) {
                Text(stringResource(R.string.share_story))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip))
            }
        }
    )
}
