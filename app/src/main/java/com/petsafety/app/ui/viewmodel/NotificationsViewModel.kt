package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.NotificationItem
import com.petsafety.app.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore = _hasMore.asStateFlow()

    /**
     * Surface transient fetch/refresh errors to the UI so the user can
     * retry. Previously fetch failure silently replaced the list with an
     * empty list — the user saw "No notifications" forever after a 502,
     * even after the network came back.
     */
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState = _errorState.asStateFlow()

    private var currentPage = 1

    fun fetchNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 1
            try {
                val (items, pagination) = repository.getNotifications(1, 20)
                _notifications.value = items
                _hasMore.value = pagination != null && pagination.page < pagination.totalPages
                _errorState.value = null
            } catch (e: Exception) {
                Timber.w(e, "fetchNotifications failed")
                // Keep prior list intact — empty list reads as "you have no
                // notifications" which is a wrong and alarming UX when the
                // real reason is a transient network failure.
                _errorState.value = e.localizedMessage ?: e.message ?: "Failed to load notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun loadMore() {
        if (!_hasMore.value || _isLoading.value) return
        viewModelScope.launch {
            currentPage++
            try {
                val (items, pagination) = repository.getNotifications(currentPage, 20)
                _notifications.value = _notifications.value + items
                _hasMore.value = pagination != null && pagination.page < pagination.totalPages
            } catch (e: Exception) {
                Timber.w(e, "loadMore failed, rolling back page")
                currentPage--
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            try {
                val (items, pagination) = repository.getNotifications(1, 20)
                _notifications.value = items
                _hasMore.value = pagination != null && pagination.page < pagination.totalPages
                fetchUnreadCount()
            } catch (e: Exception) {
                Timber.w(e, "refresh notifications failed")
            }
            _isRefreshing.value = false
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                _unreadCount.value = repository.getUnreadCount()
            } catch (e: Exception) {
                Timber.w(e, "fetchUnreadCount failed")
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(id)
                _notifications.value = _notifications.value.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
                _unreadCount.value = maxOf(0, _unreadCount.value - 1)
            } catch (e: Exception) {
                Timber.w(e, "markAsRead failed for id=%s", id)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                _unreadCount.value = 0
            } catch (e: Exception) {
                Timber.w(e, "markAllAsRead failed")
            }
        }
    }
}
