package com.cmstudio.player.ui.servers

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.library.formatCompactDuration
import com.cmstudio.player.ui.music.AudioPlayerManager
import com.cmstudio.player.ui.music.RealAudioTrack
import com.cmstudio.player.ui.theme.extendedColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MediaServerTabContent(
    kind: MediaServerKind,
    onPlayVideoUrl: (Uri, String?, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allServers by AppSettings.mediaServers.collectAsState()
    val servers = remember(allServers, kind) { allServers.filter { it.kind == kind } }

    var activeServer by remember(kind) { mutableStateOf<MediaServerProfile?>(null) }
    var showAddOrEditDialog by remember(kind) { mutableStateOf(false) }
    var editingServer by remember(kind) { mutableStateOf<MediaServerProfile?>(null) }
    var reauthServer by remember(kind) { mutableStateOf<MediaServerProfile?>(null) }

    if (activeServer != null) {
        val srv = activeServer!!
        when (kind) {
            MediaServerKind.JELLYFIN -> JellyfinBrowserPane(
                server = srv,
                onBack = { activeServer = null },
                onRequireReauth = { reauthServer = srv },
                onPlayItem = { item ->
                    val url = item.streamUrl ?: return@JellyfinBrowserPane
                    onPlayVideoUrl(Uri.parse(url), item.name, item.resumePositionMs)
                },
                modifier = modifier
            )
            MediaServerKind.NAVIDROME -> NavidromeBrowserPane(
                server = srv,
                onBack = { activeServer = null },
                onRequireReauth = { reauthServer = srv },
                modifier = modifier
            )
            MediaServerKind.AUDIOBOOKSHELF -> AudiobookshelfBrowserPane(
                server = srv,
                onBack = { activeServer = null },
                onRequireReauth = { reauthServer = srv },
                modifier = modifier
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "srv_header_${kind.id}") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.extendedColors.badgeBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (kind) {
                                MediaServerKind.JELLYFIN -> Icons.Rounded.Movie
                                MediaServerKind.NAVIDROME -> Icons.Rounded.LibraryMusic
                                MediaServerKind.AUDIOBOOKSHELF -> Icons.Rounded.Book
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(kind.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (kind) {
                                MediaServerKind.JELLYFIN -> "Self-hosted movies, shows & personal libraries"
                                MediaServerKind.NAVIDROME -> "Subsonic / OpenSubsonic music libraries"
                                MediaServerKind.AUDIOBOOKSHELF -> "Audiobooks, chapters & synced progress"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            editingServer = null
                            showAddOrEditDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
            }

            if (servers.isEmpty()) {
                item(key = "srv_empty_${kind.id}") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                            Text("No ${kind.label} Server Connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Connect your own ${kind.label} instance over LAN or HTTPS to browse and stream your authorized media.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = {
                                editingServer = null
                                showAddOrEditDialog = true
                            }) { Text("Add ${kind.label} Server") }
                        }
                    }
                }
            } else {
                items(servers, key = { it.id }) { srv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeServer = srv },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(srv.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${srv.username.ifBlank { "user" }} • ${srv.baseUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                editingServer = srv
                                showAddOrEditDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Server")
                            }
                            IconButton(onClick = { AppSettings.removeMediaServer(srv.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove Server")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddOrEditDialog) {
        MediaServerConnectDialog(
            kind = kind,
            initial = editingServer,
            onDismiss = { showAddOrEditDialog = false },
            onConnected = { profile, secret ->
                AppSettings.saveMediaServer(profile, secret)
                showAddOrEditDialog = false
                activeServer = profile
                Toast.makeText(context, "${kind.label} connected cleanly", Toast.LENGTH_SHORT).show()
            }
        )
    }

    reauthServer?.let { srv ->
        var pwd by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!busy) reauthServer = null },
            title = { Text("Sign In to ${srv.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session expired or credentials invalid for ${srv.username}.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pwd,
                        onValueChange = { pwd = it; err = null },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) {
                        Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        err = null
                        when (kind) {
                            MediaServerKind.JELLYFIN -> when (val res = JellyfinClient.authenticate(srv.baseUrl, srv.username, pwd)) {
                                is ServerCallResult.Ok -> {
                                    val updated = srv.copy(userId = res.data.second)
                                    AppSettings.saveMediaServer(updated, res.data.first)
                                    activeServer = updated
                                    reauthServer = null
                                }
                                is ServerCallResult.Err -> err = res.message
                            }
                            MediaServerKind.NAVIDROME -> when (val res = NavidromeClient.ping(srv.baseUrl, srv.username, pwd)) {
                                is ServerCallResult.Ok -> {
                                    AppSettings.saveMediaServer(srv, pwd)
                                    reauthServer = null
                                }
                                is ServerCallResult.Err -> err = res.message
                            }
                            MediaServerKind.AUDIOBOOKSHELF -> when (val res = AudiobookshelfClient.login(srv.baseUrl, srv.username, pwd)) {
                                is ServerCallResult.Ok -> {
                                    val updated = srv.copy(userId = res.data.second)
                                    AppSettings.saveMediaServer(updated, res.data.first)
                                    activeServer = updated
                                    reauthServer = null
                                }
                                is ServerCallResult.Err -> err = res.message
                            }
                        }
                        busy = false
                    }
                }) { Text(if (busy) "Signing in..." else "Sign In") }
            },
            dismissButton = { TextButton(onClick = { if (!busy) reauthServer = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MediaServerConnectDialog(
    kind: MediaServerKind,
    initial: MediaServerProfile?,
    onDismiss: () -> Unit,
    onConnected: (MediaServerProfile, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: kind.label) }
    var url by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isTesting) onDismiss() },
        title = { Text("${if (initial == null) "Connect" else "Edit"} ${kind.label}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Server URL") },
                    placeholder = {
                        Text(
                            when (kind) {
                                MediaServerKind.JELLYFIN -> "http://192.168.1.50:8096"
                                MediaServerKind.NAVIDROME -> "http://192.168.1.50:4533"
                                MediaServerKind.AUDIOBOOKSHELF -> "http://192.168.1.50:13378"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = username, onValueChange = { username = it; error = null }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password (Encrypted locally)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(enabled = !isTesting, onClick = {
                val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(url)
                if (cleanUrl.isEmpty()) {
                    error = "Enter a valid http:// or https:// URL"
                    return@Button
                }
                scope.launch {
                    isTesting = true
                    error = null
                    val id = initial?.id ?: "srv_${kind.id.lowercase(Locale.ROOT)}_${System.currentTimeMillis()}"
                    when (kind) {
                        MediaServerKind.JELLYFIN -> {
                            when (val res = JellyfinClient.authenticate(cleanUrl, username.trim(), password)) {
                                is ServerCallResult.Ok -> {
                                    onConnected(
                                        MediaServerProfile(id, kind, name.ifBlank { kind.label }, cleanUrl, username.trim(), res.data.second),
                                        res.data.first
                                    )
                                }
                                is ServerCallResult.Err -> error = res.message
                            }
                        }
                        MediaServerKind.NAVIDROME -> {
                            when (val res = NavidromeClient.ping(cleanUrl, username.trim(), password)) {
                                is ServerCallResult.Ok -> {
                                    onConnected(
                                        MediaServerProfile(id, kind, name.ifBlank { kind.label }, cleanUrl, username.trim()),
                                        password
                                    )
                                }
                                is ServerCallResult.Err -> error = res.message
                            }
                        }
                        MediaServerKind.AUDIOBOOKSHELF -> {
                            when (val res = AudiobookshelfClient.login(cleanUrl, username.trim(), password)) {
                                is ServerCallResult.Ok -> {
                                    onConnected(
                                        MediaServerProfile(id, kind, name.ifBlank { kind.label }, cleanUrl, username.trim(), res.data.second),
                                        res.data.first
                                    )
                                }
                                is ServerCallResult.Err -> error = res.message
                            }
                        }
                    }
                    isTesting = false
                }
            }) { Text(if (isTesting) "Connecting..." else "Connect") }
        },
        dismissButton = { TextButton(onClick = { if (!isTesting) onDismiss() }) { Text("Cancel") } }
    )
}

@Composable
private fun JellyfinBrowserPane(
    server: MediaServerProfile,
    onBack: () -> Unit,
    onRequireReauth: () -> Unit,
    onPlayItem: (JellyfinMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parentStack = remember { mutableStateListOf<Pair<String?, String>>(null to server.name) }
    var items by remember { mutableStateOf<List<JellyfinMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    fun load(parentId: String?) {
        scope.launch {
            isLoading = true
            err = null
            val token = AppSettings.getMediaServerSecret(server.id)
            when (val res = JellyfinClient.fetchItems(server.baseUrl, server.userId, token, parentId)) {
                is ServerCallResult.Ok -> items = res.data
                is ServerCallResult.Err -> {
                    err = res.message
                    if (res.authExpired) onRequireReauth()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(server.id, server.userId) { load(null) }

    BackHandler {
        if (parentStack.size > 1) {
            parentStack.removeAt(parentStack.lastIndex)
            load(parentStack.last().first)
        } else {
            onBack()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (parentStack.size > 1) {
                    parentStack.removeAt(parentStack.lastIndex)
                    load(parentStack.last().first)
                } else onBack()
            }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(parentStack.last().second, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(server.baseUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { load(parentStack.last().first) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            err != null -> ServerErrorBox(err!!, onRetry = { load(parentStack.last().first) }, onSignIn = onRequireReauth)
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No items in this library.") }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (item.isFolder) {
                                parentStack.add(item.id to item.name)
                                load(item.id)
                            } else {
                                onPlayItem(item)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (item.isFolder) Icons.Rounded.Folder else Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                if (!item.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(item.artworkUrl).crossfade(true).build(),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = buildString {
                                    append(item.type)
                                    if (item.runTimeMs > 0L) append(" • ${formatCompactDuration(item.runTimeMs)}")
                                    if (item.resumePositionMs > 0L) append(" • Resume ${formatCompactDuration(item.resumePositionMs)}")
                                }
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                if (item.runTimeMs > 0L && item.resumePositionMs > 0L) {
                                    LinearProgressIndicator(
                                        progress = { (item.resumePositionMs.toFloat() / item.runTimeMs).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp)
                                    )
                                }
                            }
                            if (!item.isFolder) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavidromeBrowserPane(
    server: MediaServerProfile,
    onBack: () -> Unit,
    onRequireReauth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("ALBUMS") } // ALBUMS, ARTISTS, PLAYLISTS
    var albums by remember { mutableStateOf<List<NavidromeAlbumItem>>(emptyList()) }
    var artists by remember { mutableStateOf<List<NavidromeArtistItem>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<NavidromePlaylistItem>>(emptyList()) }
    var activeSongsTitle by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf<List<NavidromeSongItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    fun loadTab() {
        scope.launch {
            isLoading = true
            err = null
            val pwd = AppSettings.getMediaServerSecret(server.id)
            when (tab) {
                "ARTISTS" -> when (val res = NavidromeClient.fetchArtists(server.baseUrl, server.username, pwd)) {
                    is ServerCallResult.Ok -> artists = res.data
                    is ServerCallResult.Err -> { err = res.message; if (res.authExpired) onRequireReauth() }
                }
                "PLAYLISTS" -> when (val res = NavidromeClient.fetchPlaylists(server.baseUrl, server.username, pwd)) {
                    is ServerCallResult.Ok -> playlists = res.data
                    is ServerCallResult.Err -> { err = res.message; if (res.authExpired) onRequireReauth() }
                }
                else -> when (val res = NavidromeClient.fetchAlbums(server.baseUrl, server.username, pwd)) {
                    is ServerCallResult.Ok -> albums = res.data
                    is ServerCallResult.Err -> { err = res.message; if (res.authExpired) onRequireReauth() }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(server.id, tab) {
        if (activeSongsTitle == null) loadTab()
    }

    BackHandler {
        if (activeSongsTitle != null) {
            activeSongsTitle = null
            songs = emptyList()
        } else onBack()
    }

    fun playNavidromeQueue(list: List<NavidromeSongItem>, idx: Int) {
        AudioPlayerManager.init(context)
        val tracks = list.map { s ->
            RealAudioTrack(
                id = "nav_${s.id}",
                uri = Uri.parse(s.streamUrl),
                title = s.title,
                artist = s.artist,
                album = s.album,
                durationMs = s.durationMs,
                durationText = formatCompactDuration(s.durationMs),
                sizeBytes = 0L,
                sizeText = "Navidrome",
                artworkUri = s.coverArtUrl?.let(Uri::parse),
                path = s.streamUrl,
                folderName = s.album,
                isRadio = true,
                streamUrl = s.streamUrl
            )
        }
        AudioPlayerManager.playQueue(tracks, idx)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (activeSongsTitle != null) {
                    activeSongsTitle = null
                    songs = emptyList()
                } else onBack()
            }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(activeSongsTitle ?: server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Navidrome • ${server.baseUrl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { loadTab() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh") }
        }
        if (activeSongsTitle == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALBUMS" to "Albums", "ARTISTS" to "Artists", "PLAYLISTS" to "Playlists").forEach { (id, lbl) ->
                    FilterChip(selected = tab == id, onClick = { tab = id }, label = { Text(lbl) })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            err != null -> ServerErrorBox(err!!, onRetry = { loadTab() }, onSignIn = onRequireReauth)
            activeSongsTitle != null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(songs.indices.toList(), key = { songs[it].id }) { idx ->
                    val s = songs[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { playNavidromeQueue(songs, idx) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${s.artist} • ${formatCompactDuration(s.durationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                        }
                    }
                }
            }
            tab == "ARTISTS" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(artists, key = { it.id }) { a ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.name, fontWeight = FontWeight.Bold)
                                Text("${a.albumCount} album(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            tab == "PLAYLISTS" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(playlists, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                isLoading = true
                                val pwd = AppSettings.getMediaServerSecret(server.id)
                                when (val res = NavidromeClient.fetchPlaylistSongs(server.baseUrl, server.username, pwd, p.id)) {
                                    is ServerCallResult.Ok -> { songs = res.data; activeSongsTitle = p.name }
                                    is ServerCallResult.Err -> err = res.message
                                }
                                isLoading = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text("${p.songCount} track(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(albums, key = { it.id }) { al ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                isLoading = true
                                val pwd = AppSettings.getMediaServerSecret(server.id)
                                when (val res = NavidromeClient.fetchAlbumSongs(server.baseUrl, server.username, pwd, al.id)) {
                                    is ServerCallResult.Ok -> { songs = res.data; activeSongsTitle = al.name }
                                    is ServerCallResult.Err -> err = res.message
                                }
                                isLoading = false
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                if (!al.coverArtUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(al.coverArtUrl).crossfade(true).build(),
                                        contentDescription = al.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(al.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${al.artist} • ${al.songCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudiobookshelfBrowserPane(
    server: MediaServerProfile,
    onBack: () -> Unit,
    onRequireReauth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var libraries by remember { mutableStateOf<List<AbsLibraryItem>>(emptyList()) }
    var selectedLibId by remember { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf<List<AbsBookItem>>(emptyList()) }
    var selectedBook by remember { mutableStateOf<AbsBookItem?>(null) }
    var absSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLoading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    fun loadInitial() {
        scope.launch {
            isLoading = true
            err = null
            val token = AppSettings.getMediaServerSecret(server.id)
            when (val res = AudiobookshelfClient.fetchLibraries(server.baseUrl, token)) {
                is ServerCallResult.Ok -> {
                    libraries = res.data
                    val targetId = selectedLibId ?: res.data.firstOrNull()?.id
                    selectedLibId = targetId
                    if (targetId != null) {
                        when (val booksRes = AudiobookshelfClient.fetchLibraryBooks(server.baseUrl, token, targetId)) {
                            is ServerCallResult.Ok -> books = booksRes.data
                            is ServerCallResult.Err -> err = booksRes.message
                        }
                    }
                }
                is ServerCallResult.Err -> {
                    err = res.message
                    if (res.authExpired) onRequireReauth()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(server.id) { loadInitial() }

    BackHandler {
        if (selectedBook != null) selectedBook = null else onBack()
    }

    fun startAudiobook(book: AbsBookItem, startMs: Long) {
        val stream = book.streamUrl ?: return
        AudioPlayerManager.init(context)
        val track = RealAudioTrack(
            id = "abs_${book.id}",
            uri = Uri.parse(stream),
            title = book.title,
            artist = book.author,
            album = "Audiobookshelf",
            durationMs = book.durationMs,
            durationText = formatCompactDuration(book.durationMs),
            sizeBytes = 0L,
            sizeText = "Audiobook",
            artworkUri = book.coverUrl?.let(Uri::parse),
            path = stream,
            folderName = book.author,
            isRadio = true,
            streamUrl = stream
        )
        AudioPlayerManager.playQueue(listOf(track), 0, startPositionMs = startMs)
        AudioPlayerManager.setPlaybackSpeed(absSpeed)
        scope.launch {
            val token = AppSettings.getMediaServerSecret(server.id)
            AudiobookshelfClient.syncProgress(server.baseUrl, token, book.id, startMs / 1000.0, book.durationMs / 1000.0)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selectedBook != null) selectedBook = null else onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(selectedBook?.title ?: server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Audiobookshelf • ${server.baseUrl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { loadInitial() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh") }
        }
        if (selectedBook == null && libraries.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                libraries.forEach { lib ->
                    FilterChip(
                        selected = selectedLibId == lib.id,
                        onClick = {
                            selectedLibId = lib.id
                            scope.launch {
                                isLoading = true
                                val token = AppSettings.getMediaServerSecret(server.id)
                                when (val r = AudiobookshelfClient.fetchLibraryBooks(server.baseUrl, token, lib.id)) {
                                    is ServerCallResult.Ok -> books = r.data
                                    is ServerCallResult.Err -> err = r.message
                                }
                                isLoading = false
                            }
                        },
                        label = { Text(lib.name) }
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            err != null -> ServerErrorBox(err!!, onRetry = { loadInitial() }, onSignIn = onRequireReauth)
            selectedBook != null -> {
                val book = selectedBook!!
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${book.author} • ${formatCompactDuration(book.durationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Speed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                                        FilterChip(selected = kotlin.math.abs(absSpeed - spd) < 0.05f, onClick = {
                                            absSpeed = spd
                                            AudioPlayerManager.setPlaybackSpeed(spd)
                                        }, label = { Text("${spd}x") })
                                    }
                                }
                                Button(
                                    onClick = { startAudiobook(book, book.currentTimeMs) },
                                    enabled = !book.streamUrl.isNullOrBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (book.currentTimeMs > 0L) "Resume (${formatCompactDuration(book.currentTimeMs)})" else "Play Audiobook")
                                }
                            }
                        }
                    }
                    if (book.chapters.isNotEmpty()) {
                        item { Text("Chapters (${book.chapters.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        items(book.chapters) { ch ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().clickable { startAudiobook(book, ch.startMs) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ch.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatCompactDuration(ch.startMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(books, key = { it.id }) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                isLoading = true
                                val token = AppSettings.getMediaServerSecret(server.id)
                                when (val d = AudiobookshelfClient.fetchBookDetail(server.baseUrl, token, book.id)) {
                                    is ServerCallResult.Ok -> selectedBook = d.data
                                    is ServerCallResult.Err -> err = d.message
                                }
                                isLoading = false
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                if (!book.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(book.coverUrl).crossfade(true).build(),
                                        contentDescription = book.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${book.author} • ${formatCompactDuration(book.durationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                if (book.durationMs > 0L && book.currentTimeMs > 0L) {
                                    LinearProgressIndicator(
                                        progress = { (book.currentTimeMs.toFloat() / book.durationMs).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerErrorBox(
    message: String,
    onRetry: () -> Unit,
    onSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Server Error", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("Retry") }
                OutlinedButton(onClick = onSignIn) { Text("Sign In") }
            }
        }
    }
}
