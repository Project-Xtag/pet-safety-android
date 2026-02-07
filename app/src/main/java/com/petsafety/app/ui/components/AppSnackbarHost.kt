package com.petsafety.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppSnackbarHost(appStateViewModel: AppStateViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appStateViewModel.snackbarMessage.collectLatest { message ->
            if (!message.isNullOrBlank()) {
                snackbarHostState.showSnackbar(message)
                appStateViewModel.clearMessage()
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                modifier = Modifier.padding(12.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White,
                actionContentColor = Color(0xFF9E9E9E) // Dark gray for action text
            ) {
                Text(text = data.visuals.message)
            }
        }
    )
}
