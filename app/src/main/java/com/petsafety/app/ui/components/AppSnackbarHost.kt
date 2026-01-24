package com.petsafety.app.ui.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

    SnackbarHost(hostState = snackbarHostState)
}
