package com.driveflow.driver.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NewsItem(
    val id: Int,
    val title: String,
    val body: String,
    val url: String?,
    val createdAt: String,
    val isUnread: Boolean
)

data class NewsUiState(
    val items: List<NewsItem> = emptyList(),
    val isLoading: Boolean = false
)

class NewsViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        loadNews()
    }

    private fun loadNews() {
        viewModelScope.launch {
            val lastSeenId = settingsStore.newsLastSeenIdFlow.first()
            val sampleNews = listOf(
                NewsItem(
                    id = 3,
                    title = "تحديث هام لقارئ إن درايف (5.189.0)",
                    body = "تم تحسين قراءة الوقفات الإضافية ومسافات البيك أب على هواتف سامسونج وأندرويد 14.",
                    url = null,
                    createdAt = "2026-10-15",
                    isUnread = lastSeenId < 3
                ),
                NewsItem(
                    id = 2,
                    title = "ميزة وضع مروح (Heading Home)",
                    body = "يمكنك الآن تحديد وجهة منزلك والتقاط المشاوير الواقعة في طريقك بكل سهولة.",
                    url = null,
                    createdAt = "2026-10-10",
                    isUnread = lastSeenId < 2
                ),
                NewsItem(
                    id = 1,
                    title = "مرحباً بكم في درايف فلو!",
                    body = "التطبيق مصمم خصيصاً لمساعدة كباتن التوصيل في اختيار المشاوير المربحة بدقة متناهية.",
                    url = null,
                    createdAt = "2026-10-01",
                    isUnread = lastSeenId < 1
                )
            )
            _uiState.value = NewsUiState(items = sampleNews)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val maxId = _uiState.value.items.maxOfOrNull { it.id } ?: 0
            settingsStore.setNewsLastSeenId(maxId)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.map { it.copy(isUnread = false) }
            )
        }
    }
}
