package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.data.network.model.CreateSuccessStoryRequest
import com.petsafety.app.data.network.model.UpdateSuccessStoryRequest
import com.petsafety.app.data.repository.SuccessStoriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuccessStoriesViewModel @Inject constructor(
    private val repository: SuccessStoriesRepository
) : ViewModel() {
    private val _stories = MutableStateFlow<List<SuccessStory>>(emptyList())
    val stories: StateFlow<List<SuccessStory>> = _stories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private var lastLatitude: Double = 51.5074
    private var lastLongitude: Double = -0.1278
    private var lastRadiusKm: Double = 100.0

    suspend fun fetchStories(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        page: Int,
        loadMore: Boolean
    ) {
        lastLatitude = latitude
        lastLongitude = longitude
        lastRadiusKm = radiusKm
        _isLoading.value = true
        try {
            val response = repository.fetchPublicStories(latitude, longitude, radiusKm, page, 10)
            _stories.value = if (loadMore) _stories.value + response.stories else response.stories
            _currentPage.value = response.page
            _hasMore.value = response.hasMore
        } catch (ex: Exception) {
            _errorMessage.value = ex.localizedMessage
        } finally {
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val response = repository.fetchPublicStories(lastLatitude, lastLongitude, lastRadiusKm, 1, 10)
                _stories.value = response.stories
                _currentPage.value = response.page
                _hasMore.value = response.hasMore
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createStory(request: CreateSuccessStoryRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val story = repository.createStory(request)
                _stories.value = listOf(story) + _stories.value
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStory(id: String, request: UpdateSuccessStoryRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val story = repository.updateStory(id, request)
                _stories.value = _stories.value.map { if (it.id == id) story else it }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStory(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteStory(id)
                _stories.value = _stories.value.filterNot { it.id == id }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
