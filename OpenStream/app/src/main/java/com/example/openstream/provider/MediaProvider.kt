package com.example.openstream.provider

import com.example.openstream.data.CatalogItem
import com.example.openstream.data.Playable

/**
 * A content source. This is OpenStream's replacement for an "extension":
 * implement it for any source you are licensed to use and register it in AppViewModel.
 */
interface MediaProvider {
    val id: String
    val name: String

    suspend fun popular(): List<CatalogItem>
    suspend fun search(query: String): List<CatalogItem>
    suspend fun sources(item: CatalogItem): List<Playable>
}
