package com.example.openstream.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.openstream.AppViewModel
import com.example.openstream.R
import com.example.openstream.data.CatalogItem
import com.example.openstream.data.Playable

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showBar = route == "home" || route == "bookmarks"

    fun openItem(item: CatalogItem) {
        vm.open(item)
        nav.navigate("detail")
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "home",
                        onClick = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(stringResource(R.string.tab_home)) }
                    )
                    NavigationBarItem(
                        selected = route == "bookmarks",
                        onClick = { nav.navigate("bookmarks") { popUpTo("home") } },
                        icon = { Icon(Icons.Default.Favorite, null) },
                        label = { Text(stringResource(R.string.tab_bookmarks)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(vm, ::openItem) }
            composable("bookmarks") { BookmarksScreen(vm, ::openItem) }
            composable("detail") {
                DetailScreen(vm, onBack = { nav.popBackStack() }, onPlay = { nav.navigate("player") })
            }
            composable("player") { PlayerScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (CatalogItem) -> Unit) {
    val state by vm.list.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf(vm.query.value) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(text) })
        )
        when {
            state.loading -> CenteredBox { CircularProgressIndicator() }
            state.error -> CenteredBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_generic))
                    OutlinedButton(onClick = { vm.search(text) }, Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            state.items.isEmpty() -> CenteredBox { Text(stringResource(R.string.no_results)) }
            else -> PosterGrid(state.items, onOpen)
        }
    }
}

@Composable
fun BookmarksScreen(vm: AppViewModel, onOpen: (CatalogItem) -> Unit) {
    val items by vm.library.bookmarks.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (items.isEmpty()) CenteredBox { Text(stringResource(R.string.no_bookmarks)) }
        else PosterGrid(items, onOpen)
    }
}

@Composable
private fun PosterGrid(items: List<CatalogItem>, onOpen: (CatalogItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(onClick = { onOpen(item) }) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                )
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun DetailScreen(vm: AppViewModel, onBack: () -> Unit, onPlay: () -> Unit) {
    val state by vm.detail.collectAsStateWithLifecycle()
    val bookmarks by vm.library.bookmarks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val item = state.item ?: return

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(0.4f).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium)
            )
            Column(Modifier.weight(0.6f)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                val saved = bookmarks.any { it.id == item.id }
                OutlinedButton(
                    onClick = { vm.library.toggleBookmark(item) },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Text(stringResource(R.string.bookmark), Modifier.padding(start = 8.dp))
                }
            }
        }
        if (item.description.isNotBlank()) {
            Text(
                item.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(16.dp)
            )
        }
        Text(
            stringResource(R.string.sources),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(16.dp))
            state.error -> Text(stringResource(R.string.error_generic), Modifier.padding(16.dp))
            state.sources.isEmpty() -> Text(stringResource(R.string.no_sources), Modifier.padding(16.dp))
            else -> state.sources.forEach { source ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        source.label,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { startDownload(context, item, source) }) {
                        Icon(Icons.Default.Download, stringResource(R.string.download))
                    }
                    Button(onClick = { vm.play(item, source); onPlay() }) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text(stringResource(R.string.play))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val playing by vm.playing.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    playing?.let { (item, source) ->
        val context = LocalContext.current
        val player = remember { ExoPlayer.Builder(context).build() }
        DisposableEffect(item.id, source.url) {
            player.setMediaItem(MediaItem.fromUri(source.url))
            player.prepare()
            player.seekTo(vm.library.position(item.id))
            player.playWhenReady = true
            onDispose {
                vm.library.savePosition(item.id, player.currentPosition)
                player.release()
            }
        }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun startDownload(context: Context, item: CatalogItem, source: Playable) {
    val fileName = Uri.parse(source.url).lastPathSegment ?: "${item.id}.mp4"
    val request = DownloadManager.Request(Uri.parse(source.url))
        .setTitle(item.title)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
}
