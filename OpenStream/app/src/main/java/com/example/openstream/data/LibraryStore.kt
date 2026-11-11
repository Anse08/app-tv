package com.example.openstream.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Local bookmarks and watch positions, kept in SharedPreferences. No accounts, no tracking. */
class LibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow(readBookmarks())
    val bookmarks: StateFlow<List<CatalogItem>> = _bookmarks

    fun isBookmarked(id: String): Boolean = _bookmarks.value.any { it.id == id }

    fun toggleBookmark(item: CatalogItem) {
        val current = _bookmarks.value
        _bookmarks.value =
            if (current.any { it.id == item.id }) current.filterNot { it.id == item.id }
            else current + item
        writeBookmarks(_bookmarks.value)
    }

    fun savePosition(itemId: String, positionMs: Long) {
        prefs.edit().putLong("pos_$itemId", positionMs).apply()
    }

    fun position(itemId: String): Long = prefs.getLong("pos_$itemId", 0L)

    private fun readBookmarks(): List<CatalogItem> {
        val raw = prefs.getString("bookmarks", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CatalogItem(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optString("description"),
                    posterUrl = o.optString("posterUrl").ifEmpty { null },
                    providerId = o.getString("providerId")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeBookmarks(items: List<CatalogItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("description", it.description)
                    .put("posterUrl", it.posterUrl ?: "")
                    .put("providerId", it.providerId)
            )
        }
        prefs.edit().putString("bookmarks", arr.toString()).apply()
    }
}
