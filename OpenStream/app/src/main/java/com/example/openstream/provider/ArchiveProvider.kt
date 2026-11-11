package com.example.openstream.provider

import com.example.openstream.data.CatalogItem
import com.example.openstream.data.Playable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Internet Archive (archive.org) public search and metadata API.
 * Free, no key. Content availability and licences vary per item.
 */
class ArchiveProvider(
    private val client: OkHttpClient = OkHttpClient()
) : MediaProvider {

    override val id = "archive"
    override val name = "Internet Archive"

    override suspend fun popular(): List<CatalogItem> =
        query("mediatype:(movies) AND collection:(feature_films)", sort = "downloads desc")

    override suspend fun search(query: String): List<CatalogItem> {
        val q = query.trim().replace("\"", " ")
        if (q.isEmpty()) return popular()
        return query("mediatype:(movies) AND (title:(\"$q\") OR subject:(\"$q\"))", sort = "downloads desc")
    }

    override suspend fun sources(item: CatalogItem): List<Playable> {
        val json = get("https://archive.org/metadata/${item.id}")
        val files = json.optJSONArray("files") ?: return emptyList()
        val out = mutableListOf<Playable>()
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val name = f.optString("name")
            if (!name.endsWith(".mp4", ignoreCase = true)) continue
            val sizeMb = f.optString("size").toLongOrNull()?.div(1_000_000)
            val label = if (sizeMb != null) "$name (${sizeMb} MB)" else name
            out += Playable(label, "https://archive.org/download/${item.id}/${name.encodeSegment()}")
        }
        return out
    }

    private suspend fun query(q: String, sort: String): List<CatalogItem> {
        val url = "https://archive.org/advancedsearch.php".toHttpUrl().newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("fl[]", "identifier")
            .addQueryParameter("fl[]", "title")
            .addQueryParameter("fl[]", "description")
            .addQueryParameter("sort[]", sort)
            .addQueryParameter("rows", "40")
            .addQueryParameter("output", "json")
            .build()
        val docs = get(url.toString()).optJSONObject("response")?.optJSONArray("docs")
            ?: return emptyList()
        return (0 until docs.length()).map { i ->
            val d = docs.getJSONObject(i)
            val id = d.getString("identifier")
            CatalogItem(
                id = id,
                title = d.optString("title", id),
                description = d.optString("description").stripHtml(),
                posterUrl = "https://archive.org/services/img/$id",
                providerId = this.id
            )
        }
    }

    private suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            JSONObject(resp.body?.string() ?: throw IOException("Empty body"))
        }
    }

    private fun String.stripHtml(): String =
        replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun String.encodeSegment(): String =
        split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
}
