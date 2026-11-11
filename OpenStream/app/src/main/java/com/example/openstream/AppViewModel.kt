package com.example.openstream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.openstream.data.CatalogItem
import com.example.openstream.data.LibraryStore
import com.example.openstream.data.Playable
import com.example.openstream.provider.ArchiveProvider
import com.example.openstream.provider.MediaProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ListState(
    val items: List<CatalogItem> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false
)

data class DetailState(
    val item: CatalogItem? = null,
    val sources: List<Playable> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    /** Register additional licensed providers here. */
    private val providers: List<MediaProvider> = listOf(ArchiveProvider())

    val library = LibraryStore(app)

    val query = MutableStateFlow("")

    private val _list = MutableStateFlow(ListState(loading = true))
    val list: StateFlow<ListState> = _list

    private val _detail = MutableStateFlow(DetailState())
    val detail: StateFlow<DetailState> = _detail

    private val _playing = MutableStateFlow<Pair<CatalogItem, Playable>?>(null)
    val playing: StateFlow<Pair<CatalogItem, Playable>?> = _playing

    private var searchJob: Job? = null

    init { search("") }

    fun search(text: String) {
        query.value = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _list.value = ListState(loading = true)
            _list.value = runCatching {
                val results = providers.flatMap { p ->
                    if (text.isBlank()) p.popular() else p.search(text)
                }
                ListState(items = results)
            }.getOrElse { ListState(error = true) }
        }
    }

    fun open(item: CatalogItem) {
        _detail.value = DetailState(item = item, loading = true)
        viewModelScope.launch {
            val provider = providers.firstOrNull { it.id == item.providerId }
            _detail.value = runCatching {
                DetailState(item = item, sources = provider?.sources(item).orEmpty())
            }.getOrElse { DetailState(item = item, error = true) }
        }
    }

    fun play(item: CatalogItem, source: Playable) {
        _playing.value = item to source
    }
}
