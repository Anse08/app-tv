package com.example.openstream.data

/** A title in a provider's catalogue. */
data class CatalogItem(
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String? = null,
    val providerId: String
)

/** One playable/downloadable file for a [CatalogItem]. */
data class Playable(
    val label: String,
    val url: String
)
