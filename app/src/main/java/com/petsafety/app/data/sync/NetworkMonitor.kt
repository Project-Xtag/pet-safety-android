package com.petsafety.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    enum class OverrideMode { SYSTEM, OFFLINE, ONLINE }
    var overrideMode: OverrideMode = OverrideMode.SYSTEM

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = overrideMode != OverrideMode.OFFLINE
        }

        override fun onLost(network: Network) {
            _isConnected.value = overrideMode == OverrideMode.ONLINE
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
        refreshStatus()
    }

    fun refreshStatus() {
        if (overrideMode != OverrideMode.SYSTEM) {
            _isConnected.value = overrideMode == OverrideMode.ONLINE
            return
        }
        val network = connectivityManager.activeNetwork ?: run {
            _isConnected.value = false
            return
        }
        val caps = connectivityManager.getNetworkCapabilities(network)
        _isConnected.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
