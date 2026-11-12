package com.cmstudio.player

import android.content.SharedPreferences
import com.cmstudio.player.diagnostics.PlaybackDiagnosticsLog
import com.cmstudio.player.engine.EngineRenderConfig
import com.cmstudio.player.engine.MpvBackendOptions
import com.cmstudio.player.engine.MpvConfManager
import com.cmstudio.player.engine.PlaybackEngineMode
import com.cmstudio.player.engine.PlaybackEngineResolver
import com.cmstudio.player.library.MediaQualityFilterType
import com.cmstudio.player.library.SmartCollectionType
import com.cmstudio.player.library.SmartCollectionsAndStats
import com.cmstudio.player.library.SmartHistoryEntry
import com.cmstudio.player.library.SmartMediaEntry
import com.cmstudio.player.player.IntroOutroManager
import com.cmstudio.player.player.IntroSegmentKind
import com.cmstudio.player.security.EncryptedCredentialStore
import com.cmstudio.player.subtitle.OnlineSubtitleClient
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.library.MediaTreeResolver
import com.cmstudio.player.ui.library.sortFolderList
import com.cmstudio.player.ui.model.MediaFolderItem
import com.cmstudio.player.ui.music.AudioTrackSortSeed
import com.cmstudio.player.ui.music.sortAudioItems
import com.cmstudio.player.ui.network.NetworkBrowserClient
import com.cmstudio.player.ui.network.NetworkPlaybackHeaders
import com.cmstudio.player.ui.player.ChannelMixMath
import com.cmstudio.player.ui.player.DoubleTapActionCatalog
import com.cmstudio.player.ui.player.EqualizerPresetsCatalog
import com.cmstudio.player.ui.player.GestureTapZone
import com.cmstudio.player.ui.player.PlayerControlCatalog
import com.cmstudio.player.ui.player.PlayerGestureZoneMath
import com.cmstudio.player.ui.player.ScreenshotTemplateFormatter
import com.cmstudio.player.ui.player.SubtitleTimingMath
import com.cmstudio.player.ui.player.VideoChapterItem
import com.cmstudio.player.ui.servers.AudiobookshelfClient
import com.cmstudio.player.ui.servers.JellyfinClient
import com.cmstudio.player.ui.servers.MediaServerUrlUtils
import com.cmstudio.player.ui.servers.NavidromeClient
import com.cmstudio.player.ui.settings.DeepSettingsSearchIndex
import com.cmstudio.player.ui.settings.SettingsDestinationPage
import com.cmstudio.player.ui.theme.ThemePreset
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MpvxCoreFeatureTest {

    @Before
    fun resetState() {
        AppSettings.resetPreferencesOnly()
        AppSettings.clearVaultPin()
    }

    @Test
    fun glassEffectIsDisabledByDefault() {
        assertFalse("Glass Effect must default to OFF", AppSettings.isLiquidGlassEnabled.value)
        AppSettings.setLiquidGlassEnabled(true)
        assertTrue(AppSettings.isLiquidGlassEnabled.value)
        AppSettings.resetAppearancePreferences()
        assertFalse(AppSettings.isLiquidGlassEnabled.value)
    }

    @Test
    fun sortingSortsFoldersNumericallyBySizeDurationAndTimestampsAndRespectsPins() {
        val folders = listOf(
            MediaFolderItem(id = "f1", name = "Beta", sizeText = "2 MB", totalSizeBytes = 2_000_000L, totalDurationMs = 90_000L, modifiedTimestamp = 2000L, dateAddedTimestamp = 100L),
            MediaFolderItem(id = "f2", name = "alpha", sizeText = "10 MB", totalSizeBytes = 10_000_000L, totalDurationMs = 30_000L, modifiedTimestamp = 1000L, dateAddedTimestamp = 300L),
            MediaFolderItem(id = "f3", name = "Gamma", sizeText = "500 KB", totalSizeBytes = 500_000L, totalDurationMs = 240_000L, modifiedTimestamp = 3000L, dateAddedTimestamp = 200L)
        )

        // Size ascending: 500 KB < 2 MB < 10 MB (numeric, NOT alphabetical "10 MB" first)
        assertEquals(listOf("f3", "f1", "f2"), sortFolderList(folders, emptySet(), "SIZE", true).map { it.id })
        assertEquals(listOf("f2", "f1", "f3"), sortFolderList(folders, emptySet(), "SIZE", false).map { it.id })

        // Duration ascending & descending
        assertEquals(listOf("f2", "f1", "f3"), sortFolderList(folders, emptySet(), "DURATION", true).map { it.id })
        assertEquals(listOf("f3", "f1", "f2"), sortFolderList(folders, emptySet(), "DURATION", false).map { it.id })

        // Date Added ascending & descending
        assertEquals(listOf("f1", "f3", "f2"), sortFolderList(folders, emptySet(), "DATE_ADDED", true).map { it.id })
        assertEquals(listOf("f2", "f3", "f1"), sortFolderList(folders, emptySet(), "DATE_ADDED", false).map { it.id })

        // Date Modified ascending & descending
        assertEquals(listOf("f2", "f1", "f3"), sortFolderList(folders, emptySet(), "DATE", true).map { it.id })

        // Case-insensitive alphabetical Name sort
        assertEquals(listOf("f2", "f1", "f3"), sortFolderList(folders, emptySet(), "TITLE", true).map { it.id })

        // Pinned folder rises to top even when sorting Z-A
        assertEquals(listOf("f2", "f3", "f1"), sortFolderList(folders, setOf("f2"), "TITLE", false).map { it.id })
    }

    @Test
    fun vaultPinRequiresExactCustomPinWithoutDefaultBackdoors() {
        assertFalse(AppSettings.hasCustomVaultPin.value)
        assertFalse(AppSettings.verifyVaultPin("0000"))
        assertFalse(AppSettings.verifyVaultPin("1234"))

        AppSettings.setVaultPin("7391")
        assertTrue(AppSettings.hasCustomVaultPin.value)
        assertTrue(AppSettings.verifyVaultPin("7391"))
        assertFalse("0000 backdoor must never bypass Vault", AppSettings.verifyVaultPin("0000"))
        assertFalse("1234 backdoor must never bypass Vault", AppSettings.verifyVaultPin("1234"))
        assertFalse(AppSettings.verifyVaultPin("0001"))

        AppSettings.clearVaultPin()
        assertFalse(AppSettings.hasCustomVaultPin.value)
        assertFalse(AppSettings.verifyVaultPin("7391"))
    }

    @Test
    fun navigationPrefsEnableDisableReorderAndRestoreDefaults() {
        AppSettings.setNavTabEnabled("HOME", false)
        assertFalse("HOME should be toggleable OFF like all other nav tabs", "HOME" in AppSettings.enabledNavTabs.value)
        AppSettings.setNavTabEnabled("HOME", true)
        assertTrue("HOME" in AppSettings.enabledNavTabs.value)

        // Guard: cannot disable the very last remaining tab
        AppSettings.ALL_NAV_TABS.drop(1).forEach { AppSettings.setNavTabEnabled(it, false) }
        val lastRemaining = AppSettings.enabledNavTabs.value.single()
        AppSettings.setNavTabEnabled(lastRemaining, false)
        assertEquals(listOf(lastRemaining), AppSettings.enabledNavTabs.value)
        AppSettings.resetNavigationPreferences()

        AppSettings.setNavTabEnabled("VAULT", false)
        assertFalse("VAULT" in AppSettings.enabledNavTabs.value)

        AppSettings.moveNavTab("MUSIC", 1)
        assertEquals("RECENTS", AppSettings.navTabOrder.value[1])
        assertEquals("MUSIC", AppSettings.navTabOrder.value[2])

        AppSettings.resetNavigationPreferences()
        assertEquals(AppSettings.ALL_NAV_TABS, AppSettings.enabledNavTabs.value)
        assertEquals(AppSettings.ALL_NAV_TABS, AppSettings.navTabOrder.value)
    }

    @Test
    fun resetPreferencesOnlyPreservesUserPlaylistsStreamsFavoritesAndVaultPin() {
        val pl = AppSettings.createPlaylist("Roadtrip Clips", "content://media/video/1")
        AppSettings.addSavedStream("Sample HLS", "https://example.com/live.m3u8", "HLS")
        AppSettings.toggleFavoriteVideo("content://media/video/1")
        AppSettings.setVaultPin("4826")
        AppSettings.setDarkTheme(false)

        AppSettings.resetPreferencesOnly()

        assertTrue("Theme pref should reset", AppSettings.isDarkTheme.value)
        assertTrue("Playlists must survive preference reset", AppSettings.userPlaylists.value.any { it.id == pl.id })
        assertTrue("Saved streams must survive preference reset", AppSettings.savedStreams.value.any { it.url == "https://example.com/live.m3u8" })
        assertTrue("Favorites must survive preference reset", "content://media/video/1" in AppSettings.favoriteVideoUris.value)
        assertTrue("Vault PIN must survive preference reset", AppSettings.verifyVaultPin("4826"))
    }

    @Test
    fun m3uParserResolvesRelativeAndAbsoluteMediaEntries() {
        val m3u = listOf(
            "#EXTM3U",
            "#EXTINF:-1,Episode 01",
            "media/ep01.mp4",
            "#EXTINF:-1,Live Stream",
            "https://cdn.example.com/stream.m3u8"
        )
        val parsed = NetworkBrowserClient.parseM3uLines("http://192.168.1.20:8080/playlists/list.m3u", m3u)
        assertEquals(2, parsed.size)
        assertEquals("Episode 01", parsed[0].name)
        assertEquals("http://192.168.1.20:8080/playlists/media/ep01.mp4", parsed[0].url)
        assertEquals("Live Stream", parsed[1].name)
        assertEquals("https://cdn.example.com/stream.m3u8", parsed[1].url)
        assertTrue(NetworkBrowserClient.isMediaOrDirectory("movie.mkv"))
        assertTrue(NetworkBrowserClient.isMediaOrDirectory("track.flac"))
        assertFalse(NetworkBrowserClient.isMediaOrDirectory("readme.txt"))
    }

    @Test
    fun localAndNetworkPlaybackHistoryRecordsMatchesAndTogglesWatchedCleanly() {
        val fakePrefs = InMemorySharedPreferences()

        // 1. Record local file:// video mid-playback (40% of 100s) -> must NOT be ignored and must stay unfinished!
        PlaybackHistoryBridge.recordEntryToPrefs(
            prefs = fakePrefs,
            uriString = "file:///sdcard/Movies/Anime/ep01.mp4",
            title = "Episode 01",
            mimeType = "video/mp4",
            positionMs = 40_000L,
            durationMs = 100_000L,
            watchedThresholdPct = 95,
            timestampMs = 1_000L
        )
        // 2. Record network stream
        PlaybackHistoryBridge.recordEntryToPrefs(
            prefs = fakePrefs,
            uriString = "https://cdn.example.com/live/master.m3u8",
            title = "Live Stream",
            mimeType = "application/x-mpegURL",
            positionMs = 15_000L,
            durationMs = 60_000L,
            watchedThresholdPct = 95,
            timestampMs = 2_000L
        )

        val records = PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs)
        assertEquals(2, records.size)
        assertEquals("https://cdn.example.com/live/master.m3u8", records[0].uriString)
        assertTrue(records[0].isNetwork)
        assertEquals("file:///sdcard/Movies/Anime/ep01.mp4", records[1].uriString)
        assertFalse(records[1].isNetwork)
        assertFalse(records[1].played)

        // Cross-scheme MediaStore URI lookup matches via normalized path / filename fallback!
        assertEquals(
            40_000L,
            PlaybackHistoryBridge.findBestSavedPosition(
                prefs = fakePrefs,
                uriString = "content://media/external/video/media/42",
                fallbackTitle = "ep01.mp4"
            )
        )
        assertNotNull(
            PlaybackHistoryBridge.findMatchingRecord(
                records = records,
                uriString = "file:///storage/emulated/0/Movies/Anime/ep01.mp4"
            )
        )

        // Crossing 95% threshold marks played = true
        PlaybackHistoryBridge.recordEntryToPrefs(
            prefs = fakePrefs,
            uriString = "file:///sdcard/Movies/Anime/ep01.mp4",
            title = "Episode 01",
            mimeType = "video/mp4",
            positionMs = 96_000L,
            durationMs = 100_000L,
            watchedThresholdPct = 95,
            timestampMs = 3_000L
        )
        val afterFinish = PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs)
            .first { !it.isNetwork }
        assertTrue("Watching >= 95% should mark local video watched", afterFinish.played)

        // Manual swipe toggle unwatch resets played = false and position = 0
        PlaybackHistoryBridge.setWatchedStateInPrefs(
            prefs = fakePrefs,
            uriString = "file:///sdcard/Movies/Anime/ep01.mp4",
            title = "Episode 01",
            durationMs = 100_000L,
            watched = false,
            timestampMs = 4_000L
        )
        val afterUnwatch = PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs)
            .first { !it.isNetwork }
        assertFalse(afterUnwatch.played)
        assertEquals(0L, afterUnwatch.positionMs)

        // Remove single entry & clear all
        PlaybackHistoryBridge.removeEntryFromPrefs(fakePrefs, "https://cdn.example.com/live/master.m3u8")
        assertEquals(1, PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs).size)
        PlaybackHistoryBridge.clearAllHistoryPrefs(fakePrefs)
        assertTrue(PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs).isEmpty())
    }

    @Test
    fun treePathSegmentsAndTitleFormattingResolveCleanly() {
        assertEquals(
            listOf("Movies", "SciFi", "Classics"),
            MediaTreeResolver.splitPathSegments("Movies/SciFi/Classics/", "Classics")
        )
        assertEquals(
            listOf("Downloads"),
            MediaTreeResolver.splitPathSegments("", "Downloads")
        )
        assertEquals("movie_sample", MediaTreeResolver.formatVideoTitle("movie_sample", "movie_sample.mkv", false))
        assertEquals("movie_sample.mkv", MediaTreeResolver.formatVideoTitle("movie_sample", "movie_sample.mkv", true))
    }

    @Test
    fun phase2GridColumnStealthVaultAndSecurityQuestionWorkAndSurvivePrefReset() {
        AppSettings.setFolderGridColumnsPortrait(99)
        AppSettings.setVideoGridColumnsLandscape(1)
        assertEquals(4, AppSettings.folderGridColumnsPortrait.value)
        assertEquals(2, AppSettings.videoGridColumnsLandscape.value)

        assertTrue(AppSettings.showQuickPlayFab.value)
        assertFalse(AppSettings.quickPlayFabDirect.value)
        AppSettings.setQuickPlayFabDirect(true)

        AppSettings.setVaultPin("2468")
        AppSettings.setVaultEntryHidden(true)
        AppSettings.setVaultSecurityQuestion("First pet?", "Rex")
        assertTrue(AppSettings.hasVaultSecurityQuestion())
        assertTrue(AppSettings.verifyVaultSecurityAnswer("  rex  "))
        assertFalse(AppSettings.verifyVaultSecurityAnswer("Max"))

        AppSettings.resetPreferencesOnly()

        assertFalse("Direct FAB pref resets with library prefs", AppSettings.quickPlayFabDirect.value)
        assertTrue("Vault security question survives pref reset", AppSettings.verifyVaultSecurityAnswer("rex"))
        assertTrue("Vault PIN survives pref reset", AppSettings.verifyVaultPin("2468"))
    }

    @Test
    fun channelMixMathProcessesMonoReverseStereoAndAutoCleanly() {
        val original = shortArrayOf(1000, -500, 2000, 4000)

        val autoCopy = original.clone()
        ChannelMixMath.processPcm16Samples(autoCopy, autoCopy.size, 2, ChannelMixMath.MODE_AUTO)
        assertEquals(1000, autoCopy[0].toInt())
        assertEquals(-500, autoCopy[1].toInt())

        val revCopy = original.clone()
        ChannelMixMath.processPcm16Samples(revCopy, revCopy.size, 2, ChannelMixMath.MODE_REVERSE_STEREO)
        assertEquals(-500, revCopy[0].toInt())
        assertEquals(1000, revCopy[1].toInt())
        assertEquals(4000, revCopy[2].toInt())
        assertEquals(2000, revCopy[3].toInt())

        val monoCopy = original.clone()
        ChannelMixMath.processPcm16Samples(monoCopy, monoCopy.size, 2, ChannelMixMath.MODE_MONO)
        assertEquals(250, monoCopy[0].toInt())
        assertEquals(250, monoCopy[1].toInt())
        assertEquals(3000, monoCopy[2].toInt())
        assertEquals(3000, monoCopy[3].toInt())
    }

    @Test
    fun subtitleTimingMathScalesSpeedAndDelaysCleanly() {
        // 10s @ 1.0x with +500ms delay -> 9.5s (delays cues by looking slightly earlier)
        assertEquals(
            9_500_000L,
            SubtitleTimingMath.adjustedPositionUs(10_000_000L, delayMs = 500, speedMilli = 1000)
        )
        // 10s @ 1.5x with 0ms delay -> 15s
        assertEquals(
            15_000_000L,
            SubtitleTimingMath.adjustedPositionUs(10_000_000L, delayMs = 0, speedMilli = 1500)
        )
        // Speed clamp bounds 0.1x..10.0x
        assertEquals(100, SubtitleTimingMath.clampSpeedMilli(10))
        assertEquals(10000, SubtitleTimingMath.clampSpeedMilli(25000))
    }

    @Test
    fun equalizerPresetsCatalogAndPlayerControlSlotEditorWorkCleanly() {
        assertEquals(12, EqualizerPresetsCatalog.PRESETS.size)
        val rockBands = EqualizerPresetsCatalog.bandsForPreset("Rock", "")
        assertEquals(listOf(5f, 3f, -1f, 3f, 5f), rockBands)

        val serialized = EqualizerPresetsCatalog.serializeBands(listOf(8f, -4f, 0f, 3.5f, 20f))
        assertEquals("8.0,-4.0,0.0,3.5,12.0", serialized)
        assertEquals(listOf(8f, -4f, 0f, 3.5f, 12f), EqualizerPresetsCatalog.bandsForPreset("Custom", serialized))

        val moved = PlayerControlCatalog.moveTokenInCsv("AUTO,MUSIC,SUBTITLES", "SUBTITLES", -1)
        assertEquals("DECODER,SUBTITLES,AUDIO_TRACK", moved)

        val toggled = PlayerControlCatalog.toggleTokenInCsv(moved, "EQUALIZER")
        assertEquals("DECODER,SUBTITLES,AUDIO_TRACK,EQUALIZER", toggled)

        val dupes = PlayerControlCatalog.findDuplicateTokens(listOf("DECODER,MORE", "MORE,LOCK"))
        assertTrue("MORE_OPTIONS" in dupes)
    }

    @Test
    fun gestureZoneMappingAndPhase34AppSettingsPersistAndResetCleanly() {
        assertEquals(GestureTapZone.LEFT, PlayerGestureZoneMath.zoneForTapX(150f, 1000f, 28))
        assertEquals(GestureTapZone.CENTER, PlayerGestureZoneMath.zoneForTapX(500f, 1000f, 28))
        assertEquals(GestureTapZone.RIGHT, PlayerGestureZoneMath.zoneForTapX(850f, 1000f, 28))

        AppSettings.setSeekbarStyle("THICK")
        AppSettings.setPlayerControlsTheme("GLOSSY")
        AppSettings.setDisplayVolumeOnRight(false)
        AppSettings.setRememberBrightness(true)
        AppSettings.setHoldSpeedMultiplier(3.5f)
        AppSettings.setCustomSkipDurationSec(120)
        AppSettings.setAudioChannelMode(ChannelMixMath.MODE_MONO)
        AppSettings.setSubtitleSpeedMilli(1250)
        AppSettings.setSubtitleItalic(true)

        assertEquals("THICK", AppSettings.seekbarStyle.value)
        assertEquals("GLOSSY", AppSettings.playerControlsTheme.value)
        assertFalse(AppSettings.displayVolumeOnRight.value)
        assertEquals(120, AppSettings.customSkipDurationSec.value)

        AppSettings.resetPreferencesOnly()

        assertEquals("NORMAL", AppSettings.seekbarStyle.value)
        assertEquals("GLASS", AppSettings.playerControlsTheme.value)
        assertTrue(AppSettings.displayVolumeOnRight.value)
        assertEquals(90, AppSettings.customSkipDurationSec.value)
        assertEquals(1000, AppSettings.subtitleSpeedMilli.value)
        assertEquals(ChannelMixMath.MODE_AUTO, AppSettings.audioChannelMode.value)
    }

    @Test
    fun phase5And6PlaybackDeduplicationHeadersMusicSortAndPlaylistReorderWork() {
        val fakePrefs = InMemorySharedPreferences()

        // 1. Local content:// and file:// URIs for the same file deduplicate cleanly in Recents
        PlaybackHistoryBridge.recordEntryToPrefs(
            prefs = fakePrefs,
            uriString = "file:///storage/emulated/0/Movies/sample_test.mp4",
            title = "sample_test",
            mimeType = "video/mp4",
            positionMs = 15_000L,
            durationMs = 120_000L,
            rememberPosition = true
        )
        PlaybackHistoryBridge.recordEntryToPrefs(
            prefs = fakePrefs,
            uriString = "content://media/external/video/media/991",
            title = "sample_test",
            mimeType = "video/mp4",
            positionMs = 42_000L,
            durationMs = 120_500L,
            rememberPosition = true
        )
        val deduped = PlaybackHistoryBridge.loadLocalRecordsFromPrefs(fakePrefs)
        assertEquals("Same local file via file:// and content:// must collapse to 1 record", 1, deduped.size)
        assertEquals(42_000L, deduped.first().positionMs)
        assertEquals("content://media/external/video/media/991", deduped.first().uriString)

        // 2. Network playback HTTP headers & origin matching
        NetworkPlaybackHeaders.clearSessionHeaders()
        val hdrs = NetworkBrowserClient.buildPlaybackHeaders(
            username = "alice",
            password = "secret",
            userAgent = "CustomAgent/2.0",
            extraHeaders = mapOf("Referer" to "https://cdn.example.org/list.m3u")
        )
        assertTrue(hdrs["Authorization"]!!.startsWith("Basic "))
        assertEquals("CustomAgent/2.0", hdrs["User-Agent"])
        assertEquals("https://cdn.example.org/list.m3u", hdrs["Referer"])
        NetworkPlaybackHeaders.registerForUrl("https://cdn.example.org/videos/movie.mkv", hdrs)
        val resolvedSibling = NetworkPlaybackHeaders.getHeadersForUrl("https://cdn.example.org/sub/ep2.mkv")
        assertEquals(hdrs["Authorization"], resolvedSibling["Authorization"])

        // 3. Music sorting by title/artist/duration + tab visibility/order
        val tracks = listOf(
            AudioTrackSortSeed("1", "Zebra", "Bob", "Al1", 210_000L),
            AudioTrackSortSeed("2", "Alpha", "Charlie", "Al2", 90_000L),
            AudioTrackSortSeed("3", "Middle", "Alice", "Al3", 300_000L)
        )
        val byArtist = sortAudioItems(tracks, "ARTIST", true, { it.id }, { it.title }, { it.artist }, { it.album }, { it.durationMs })
        assertEquals(listOf("3", "1", "2"), byArtist.map { it.id })
        val byDurDesc = sortAudioItems(tracks, "DURATION", false, { it.id }, { it.title }, { it.artist }, { it.album }, { it.durationMs })
        assertEquals(listOf("3", "1", "2"), byDurDesc.map { it.id })

        AppSettings.toggleMusicTabVisibility("ALBUMS")
        assertTrue("ALBUMS" in AppSettings.musicHiddenTabs.value)
        AppSettings.moveMusicTab("PLAYLISTS", -1)
        assertTrue(AppSettings.musicTabOrder.value.indexOf("PLAYLISTS") < AppSettings.ALL_MUSIC_TABS.indexOf("PLAYLISTS"))

        // 4. Playlist reordering & network bookmarks
        val pl = AppSettings.createPlaylist("Action Mix")
        AppSettings.addVideoToPlaylist(pl.id, "content://vid/1")
        AppSettings.addVideoToPlaylist(pl.id, "content://vid/2")
        AppSettings.moveVideoInPlaylist(pl.id, 1, -1)
        val updatedPl = AppSettings.userPlaylists.value.first { it.id == pl.id }
        assertEquals(listOf("content://vid/2", "content://vid/1"), updatedPl.itemUris)
        AppSettings.deletePlaylist(pl.id)

        AppSettings.addNetworkBookmark("srv1", "Anime Folder", "http://192.168.1.10/anime/", true)
        assertEquals(1, AppSettings.networkBookmarks.value.size)
        AppSettings.removeNetworkBookmark("http://192.168.1.10/anime/")
        assertTrue(AppSettings.networkBookmarks.value.isEmpty())
    }

    @Test
    fun phase7And8DeepSettingsSearchSystemFontAndJsonBackupWork() {
        // 1. Deep Settings Search has 100+ items, NFD accent folding & accurate relevance scoring
        assertTrue(
            "Deep settings search index must cover 100+ real controls",
            DeepSettingsSearchIndex.entries.size >= 100
        )
        val wavyHits = DeepSettingsSearchIndex.search("wavy seekbar")
        assertTrue(wavyHits.any { it.page == SettingsDestinationPage.VIDEO_PLAYER })
        val nfdResumeHits = DeepSettingsSearchIndex.search("r\u00e9sum\u00e9")
        assertTrue(nfdResumeHits.any { it.page == SettingsDestinationPage.GENERAL })
        val backupHits = DeepSettingsSearchIndex.search("backup json")
        assertTrue(backupHits.any { it.page == SettingsDestinationPage.ADVANCED })
        val statsHits = DeepSettingsSearchIndex.search("vulkan opengl")
        assertTrue(statsHits.any { it.page == SettingsDestinationPage.ABOUT })

        // 2. Search history deduplication & cap
        AppSettings.clearSettingsSearchHistory()
        AppSettings.recordSettingsSearchQuery("seekbar")
        AppSettings.recordSettingsSearchQuery("amoled")
        AppSettings.recordSettingsSearchQuery("Seekbar")
        assertEquals(listOf("Seekbar", "amoled"), AppSettings.settingsSearchHistory.value)

        // 3. Pure-JVM typed JSON Backup & Restore + Sensitive key stripping
        val srcPrefs = InMemorySharedPreferences()
        srcPrefs.edit()
            .putBoolean("pref_amoled", true)
            .putBoolean("pref_use_system_font", true)
            .putInt("pref_custom_skip_sec", 120)
            .putInt("pref_sub_speed_milli", 1450)
            .putString("pref_seekbar_style", "WAVY")
            .putString("pref_nav_enabled_tabs", "HOME,MUSIC,NETWORK")
            .putStringSet("pref_music_hidden_tabs", setOf("ALBUMS"))
            .putString("pref_vault_pin_hash", "SENSITIVE_PIN_HASH")
            .putString("pref_vault_sec_answer_hash", "SENSITIVE_ANSWER_HASH")
            .putString("pref_gemini_key", "SENSITIVE_API_SECRET")
            .apply()

        val exportedJson = AppSettings.exportSettingsJson(srcPrefs)
        assertTrue("pref_amoled" in exportedJson)
        assertTrue("pref_use_system_font" in exportedJson)
        assertFalse("Vault PIN hash must NEVER be exported", "SENSITIVE_PIN_HASH" in exportedJson)
        assertFalse("Vault answer hash must NEVER be exported", "SENSITIVE_ANSWER_HASH" in exportedJson)
        assertFalse("API key must NEVER be exported", "SENSITIVE_API_SECRET" in exportedJson)

        val destPrefs = InMemorySharedPreferences()
        val restoredCount = AppSettings.importSettingsJson(exportedJson, destPrefs)
        assertTrue(restoredCount >= 7)
        assertTrue(AppSettings.isAmoled.value)
        assertTrue(AppSettings.useSystemFont.value)
        assertEquals(120, AppSettings.customSkipDurationSec.value)
        assertEquals(1450, AppSettings.subtitleSpeedMilli.value)
        assertEquals("WAVY", AppSettings.seekbarStyle.value)
        assertEquals(listOf("HOME", "MUSIC", "NETWORK"), AppSettings.enabledNavTabs.value)
        assertEquals(setOf("ALBUMS"), AppSettings.musicHiddenTabs.value)
        assertEquals(-1, AppSettings.importSettingsJson("{bad json}", destPrefs))
    }

    @Test
    fun dualEngineResolverRoutesHdrStreamsExplicitOverridesCleanly() {
        // AUTO defaults local SDR to MPV
        assertEquals(
            PlaybackEngineMode.MPV,
            PlaybackEngineResolver.resolveEffectiveEngine(
                configuredGlobal = PlaybackEngineMode.AUTO,
                perVideoOverride = null,
                uriString = "file:///storage/emulated/0/Movies/Anime_Episode_01.mkv",
                title = "Anime Episode 01",
                autoRouteDolbyVision = true,
                autoRouteStreams = true
            )
        )
        // AUTO routes HDR10 / Dolby Vision to NATIVE (ExoPlayer)
        assertEquals(
            PlaybackEngineMode.NATIVE,
            PlaybackEngineResolver.resolveEffectiveEngine(
                configuredGlobal = PlaybackEngineMode.AUTO,
                perVideoOverride = null,
                uriString = "file:///storage/emulated/0/Movies/Dune.2160p.DV.DoVi.mkv",
                title = "Dune 4K DV",
                autoRouteDolbyVision = true,
                autoRouteStreams = true
            )
        )
        // AUTO routes HLS/HTTP streams to NATIVE when autoRouteStreams = true, else MPV
        assertEquals(
            PlaybackEngineMode.NATIVE,
            PlaybackEngineResolver.resolveEffectiveEngine(
                configuredGlobal = PlaybackEngineMode.AUTO,
                perVideoOverride = null,
                uriString = "https://example.com/live/master.m3u8",
                title = "Live Stream",
                autoRouteDolbyVision = true,
                autoRouteStreams = true
            )
        )
        assertEquals(
            PlaybackEngineMode.MPV,
            PlaybackEngineResolver.resolveEffectiveEngine(
                configuredGlobal = PlaybackEngineMode.AUTO,
                perVideoOverride = null,
                uriString = "https://example.com/live/master.m3u8",
                title = "Live Stream",
                autoRouteDolbyVision = true,
                autoRouteStreams = false
            )
        )
        // Per-video override beats global preference
        assertEquals(
            PlaybackEngineMode.MPV,
            PlaybackEngineResolver.resolveEffectiveEngine(
                configuredGlobal = PlaybackEngineMode.NATIVE,
                perVideoOverride = PlaybackEngineMode.MPV,
                uriString = "https://example.com/live/master.m3u8",
                title = "Live Stream",
                autoRouteDolbyVision = true,
                autoRouteStreams = true
            )
        )
    }

    @Test
    fun mpvBackendOptionsProduceValidAudioFiltersAnime4kAndBackendConfigs() {
        assertEquals("mediacodec,mediacodec-copy", MpvBackendOptions.preferredHwdecMode("HW_ONLY"))
        assertEquals("no", MpvBackendOptions.preferredHwdecMode("SW_PREFER"))
        assertEquals("mediacodec-copy,mediacodec,no", MpvBackendOptions.preferredHwdecMode("AUTO"))

        val vkBackend = MpvBackendOptions.resolveVideoBackend(EngineRenderConfig(useVulkan = true, useGpuNext = true))
        assertEquals(Triple("gpu-next", "vulkan", "androidvk"), vkBackend)

        val glFallback = MpvBackendOptions.resolveVideoBackend(
            EngineRenderConfig(useVulkan = true, forceOpenGlFallback = true, useGpuNext = true, anime4kEnabled = true)
        )
        assertEquals(Triple("gpu", "opengl", "android"), glFallback)

        val af = MpvBackendOptions.buildAudioFilterChain(
            eqBandsDb = listOf(4f, 0f, -2f, 0f, 3f),
            channelMode = "MONO",
            nightCompressor = true
        )
        assertTrue(af.startsWith("lavfi=["))
        assertTrue(af.contains("equalizer=f=60"))
        assertTrue(af.contains("pan=mono"))
        assertTrue(af.contains("acompressor"))

        val modeA = MpvBackendOptions.resolveAnime4kShaderNames(true, "MODE_A", "HQ")
        assertTrue(modeA.contains("Anime4K_Clamp_Highlights.glsl"))
        assertTrue(modeA.none { it.contains("Ani4Kv2") })
        assertTrue(MpvBackendOptions.resolveAnime4kShaderNames(false, "MODE_A", "HQ").isEmpty())

        val headers = MpvBackendOptions.buildHttpHeaderOption(mapOf("User-Agent" to "MPVX/2.0", "Referer" to "https://a,b"))
        assertTrue(headers.contains("User-Agent: MPVX/2.0"))
        assertTrue(headers.contains("https://a\\,b"))
    }

    @Test
    fun all42ThemePresetsDarkModeWallpaperAndBackupRoundtripWorkCleanly() {
        assertEquals(43, ThemePreset.entries.size)
        val displayNames = ThemePreset.entries.map { it.displayName }
        assertEquals("Every preset must have a unique label", displayNames.size, displayNames.toSet().size)
        ThemePreset.entries.forEach { preset ->
            assertTrue("Preset ${preset.name} needs label", preset.displayName.isNotBlank())
            assertTrue(
                "Light preset ${preset.name} needs distinct primary vs background",
                abs(preset.primaryLight.luminance() - preset.backgroundLight.luminance()) > 0.08f
            )
            assertTrue(
                "Dark preset ${preset.name} needs distinct primary vs background",
                abs(preset.primaryDark.luminance() - preset.backgroundDark.luminance()) > 0.08f
            )
        }

        // 3-way DarkMode
        AppSettings.setDarkMode("LIGHT")
        assertEquals("LIGHT", AppSettings.darkMode.value)
        assertFalse(AppSettings.isDarkTheme.value)
        AppSettings.setDarkMode("DARK")
        assertEquals("DARK", AppSettings.darkMode.value)
        assertTrue(AppSettings.isDarkTheme.value)
        AppSettings.setDarkMode("SYSTEM")
        assertEquals("SYSTEM", AppSettings.darkMode.value)

        // Favorite theme toggle
        AppSettings.toggleFavoriteThemePreset("MOCHA")
        assertTrue("MOCHA" in AppSettings.favoriteThemePresets.value)
        AppSettings.toggleFavoriteThemePreset("MOCHA")
        assertFalse("MOCHA" in AppSettings.favoriteThemePresets.value)

        // Wallpaper clamping + backup export/import roundtrip (incl. sensitive key stripping)
        val srcPrefs = InMemorySharedPreferences()
        srcPrefs.edit()
            .putString("pref_dark_mode", "LIGHT")
            .putString("pref_theme_preset", "KANAGAWA")
            .putStringSet("pref_favorite_themes", setOf("KANAGAWA", "NORD"))
            .putString("pref_custom_wallpaper_uri", "file:///storage/emulated/0/Pictures/bg.jpg")
            .putBoolean("pref_custom_wallpaper_is_video", false)
            .putFloat("pref_custom_wallpaper_blur", 18f)
            .putFloat("pref_custom_wallpaper_brightness", 0.85f)
            .putFloat("pref_custom_wallpaper_saturation", 1.15f)
            .putFloat("pref_custom_wallpaper_scrim", 0.45f)
            .putFloat("pref_custom_surface_opacity", 0.82f)
            .putBoolean("pref_show_rotation_overlay", false)
            .putString("pref_screenshot_format", "WEBP")
            .putInt("pref_screenshot_quality", 88)
            .putString("pref_screenshot_folder", "AnimeShots")
            .putString("pref_gemini_key", "secret-should-strip")
            .putString("pref_vault_pin_hash", "hash-should-strip")
            .commit()

        val exportedJson = AppSettings.exportSettingsJson(srcPrefs)
        assertFalse("Sensitive API key must be stripped from backup", exportedJson.contains("secret-should-strip"))
        assertFalse("Vault hash must be stripped from backup", exportedJson.contains("hash-should-strip"))

        val dstPrefs = InMemorySharedPreferences()
        val importedCount = AppSettings.importSettingsJson(exportedJson, dstPrefs)
        assertTrue("Expected restored keys", importedCount >= 14)
        assertEquals("LIGHT", AppSettings.darkMode.value)
        assertEquals(ThemePreset.KANAGAWA, AppSettings.themePreset.value)
        assertTrue("KANAGAWA" in AppSettings.favoriteThemePresets.value)
        assertEquals("file:///storage/emulated/0/Pictures/bg.jpg", AppSettings.customThemeWallpaperUri.value)
        assertEquals(18f, AppSettings.customThemeWallpaperBlur.value, 0.01f)
        assertEquals(0.85f, AppSettings.customThemeWallpaperBrightness.value, 0.01f)
        assertEquals(1.15f, AppSettings.customThemeWallpaperSaturation.value, 0.01f)
        assertEquals(0.45f, AppSettings.customThemeWallpaperScrimAlpha.value, 0.01f)
        assertEquals(0.82f, AppSettings.customThemeSurfaceOpacity.value, 0.01f)
        assertFalse(AppSettings.showScreenRotationOverlay.value)
        assertEquals("WEBP", AppSettings.screenshotFormat.value)
        assertEquals(88, AppSettings.screenshotQuality.value)
        assertEquals("AnimeShots", AppSettings.screenshotFolder.value)
    }

    @Test
    fun hudSlotExclusivityOverlayTogglesScreenshotPrefsAndSearchIndexWork() {
        // Cross-slot landscape exclusivity
        AppSettings.assignHudSlotControls(
            slot = "TOP_RIGHT",
            tokens = listOf("SCREEN_ROTATION", "ASPECT_RATIO", "PLAYBACK_SPEED"),
            enforceLandscapeExclusivity = true
        )
        val topRight = PlayerControlCatalog.parseSlotCsv(AppSettings.topRightHudControls.value)
        val bottomLeft = PlayerControlCatalog.parseSlotCsv(AppSettings.bottomLeftHudControls.value)
        val bottomRight = PlayerControlCatalog.parseSlotCsv(AppSettings.bottomRightHudControls.value)
        assertEquals(listOf("SCREEN_ROTATION", "ASPECT_RATIO", "PLAYBACK_SPEED"), topRight)
        assertTrue(topRight.none { it in bottomLeft } && topRight.none { it in bottomRight })

        // Portrait slot should not clear landscape slots
        AppSettings.assignHudSlotControls(
            slot = "PORTRAIT_BOTTOM",
            tokens = listOf("SCREEN_ROTATION", "AUDIO_TRACK"),
            enforceLandscapeExclusivity = true
        )
        assertTrue("SCREEN_ROTATION" in PlayerControlCatalog.parseSlotCsv(AppSettings.topRightHudControls.value))

        // Overlay visibility toggles & screenshot preferences + reset
        assertTrue(AppSettings.showAspectRatioOverlay.value)
        assertTrue(AppSettings.showScreenshotOverlay.value)
        AppSettings.setShowAspectRatioOverlay(false)
        AppSettings.setShowDecoderOverlay(false)
        AppSettings.setShowShuffleOverlay(false)
        AppSettings.setShowLoopOverlay(false)
        AppSettings.setShowPipOverlay(false)
        AppSettings.setShowScreenshotOverlay(false)
        AppSettings.setScreenshotFormat("jpeg")
        AppSettings.setScreenshotQuality(42)
        AppSettings.setScreenshotFolder("CustomCaps")
        assertEquals("JPEG", AppSettings.screenshotFormat.value)
        assertEquals(60, AppSettings.screenshotQuality.value)
        assertEquals("CustomCaps", AppSettings.screenshotFolder.value)

        AppSettings.resetPreferencesOnly()
        assertTrue(AppSettings.showAspectRatioOverlay.value)
        assertTrue(AppSettings.showDecoderOverlay.value)
        assertTrue(AppSettings.showScreenshotOverlay.value)
        assertEquals("PNG", AppSettings.screenshotFormat.value)
        assertEquals(95, AppSettings.screenshotQuality.value)
        assertEquals("MPVX Screenshots", AppSettings.screenshotFolder.value)

        // Settings search entries for newly added features
        val anime4kHits = DeepSettingsSearchIndex.search("anime4k")
        assertTrue("Search 'anime4k' must find Decoder Engine settings", anime4kHits.any { it.page == SettingsDestinationPage.DECODER_ENGINE })
        val vulkanHits = DeepSettingsSearchIndex.search("vulkan")
        assertTrue("Search 'vulkan' must find Decoder Engine or About settings", vulkanHits.any { it.page == SettingsDestinationPage.DECODER_ENGINE })
        val wallpaperHits = DeepSettingsSearchIndex.search("wallpaper")
        assertTrue("Search 'wallpaper' must find Appearance settings", wallpaperHits.any { it.page == SettingsDestinationPage.APPEARANCE })
    }

    @Test
    fun expandedSeekbarsScreenshotTemplatesAndDualEngineParityPrefsWorkCleanly() {
        // 1. 9 Seekbar styles including Segment / Dual Glow / Pulse Bar
        val styleIds = PlayerControlCatalog.SEEKBAR_STYLES.map { it.first }
        assertTrue(styleIds.size >= 9)
        assertTrue("SEGMENTED" in styleIds)
        assertTrue("DUAL_GLOW" in styleIds)
        assertTrue("PULSE_BAR" in styleIds)

        // 2. Screenshot filename template substitution & path traversal sanitization
        val formatted = ScreenshotTemplateFormatter.formatFilename(
            template = "Frame_{title}_{date}_{time}",
            videoTitle = "../Movie Clip/S01E02:Final!.mkv",
            timestampText = "20261111_120000",
            dateText = "2026-11-11",
            extension = "png"
        )
        assertFalse(formatted.contains(".."))
        assertFalse(formatted.contains("/"))
        assertTrue(formatted.endsWith(".png"))
        assertTrue(formatted.contains("2026-11-11_20261111_120000"))

        // 3. Double-tap per-zone actions & audio normalization FFmpeg filter chain
        assertEquals(DoubleTapActionCatalog.ACTION_CUSTOM_SKIP, DoubleTapActionCatalog.normalizeAction("custom_skip", "SEEK"))
        assertEquals(DoubleTapActionCatalog.ACTION_SEEK, DoubleTapActionCatalog.normalizeAction("invalid_action", "SEEK"))
        val afNorm = MpvBackendOptions.buildAudioFilterChain(
            eqBandsDb = listOf(0f, 0f, 0f, 0f, 0f),
            channelMode = "AUTO",
            nightCompressor = false,
            normalizeVolume = true
        )
        assertTrue(afNorm.contains("dynaudnorm"))

        // 4. Dual-engine parity toggles + Boost cap + Subtitle ASS override + Extra Features
        AppSettings.setAudioBoost(190)
        AppSettings.setVolumeBoostCapPct(150)
        assertEquals(150, AppSettings.audioBoost.value)
        AppSettings.setSubOverrideAssMode("FORCE")
        assertEquals("FORCE", AppSettings.subOverrideAssMode.value)
        AppSettings.setSubScaleByWindow(false)
        assertFalse(AppSettings.subScaleByWindow.value)
        AppSettings.setIncludeSubtitlesInScreenshot(false)
        assertFalse(AppSettings.includeSubtitlesInScreenshot.value)
        AppSettings.setControlsDrawerEnabled(false)
        assertFalse(AppSettings.controlsDrawerEnabled.value)
        AppSettings.setShowSeekRipple(false)
        assertFalse(AppSettings.showSeekRipple.value)

        AppSettings.setExtraSmartCollections(true)
        AppSettings.setExtraWatchStats(true)
        assertTrue(AppSettings.extraSmartCollections.value)
        assertTrue(AppSettings.extraWatchStats.value)

        // 5. Deep search discovers Extra Features & Supported Codecs cleanly
        val codecHits = DeepSettingsSearchIndex.search("hevc codecs")
        assertTrue(codecHits.any { it.page == SettingsDestinationPage.SUPPORTED_CODECS })
        val extraHits = DeepSettingsSearchIndex.search("smart collections")
        assertTrue(extraHits.any { it.page == SettingsDestinationPage.EXTRA_FEATURES })

        // 6. Category reset restores subtitle & playback defaults safely
        AppSettings.resetSubtitlePreferences()
        assertEquals("SCALE", AppSettings.subOverrideAssMode.value)
        assertTrue(AppSettings.subScaleByWindow.value)
        AppSettings.resetPlaybackAndPlayerPreferences()
        assertTrue(AppSettings.includeSubtitlesInScreenshot.value)
        assertTrue(AppSettings.controlsDrawerEnabled.value)
    }

    @Test
    fun mpvConfManagerWhitelistsValidOptionsAndRejectsUnsafeOnes() {
        val raw = """
            # Safe user config
            deband=yes
            deband-iterations=2
            tone-mapping=bt.2446a
            interpolation=yes
            sub-auto=fuzzy
            # Unsafe / blocked keys
            script=/data/local/tmp/evil.lua
            ao=null
            vo=null
            ytdl=yes
            input-ipc-server=/tmp/mpvsocket
            unknown-bogus-key=123
        """.trimIndent()

        val result = MpvConfManager.validateAndParse(raw)
        assertFalse(result.isClean)
        assertEquals(5, result.validEntries.size)
        assertTrue(result.validEntries.any { it.key == "deband" && it.value == "yes" })
        assertTrue(result.validEntries.any { it.key == "sub-auto" && it.value == "fuzzy" })
        assertTrue("deband" in result.ownedKeys)
        assertFalse("script" in result.ownedKeys)
        assertFalse("ytdl" in result.ownedKeys)
        assertTrue(result.diagnostics.count { it.isError } >= 5)
        assertEquals("Subtitles", MpvConfManager.ownershipGroupForKey("sub-auto"))
        assertEquals("Decoder & Engine", MpvConfManager.ownershipGroupForKey("hwdec"))
    }

    @Test
    fun playbackDiagnosticsLogRecordsFormatsAndClearsEntriesCleanly() {
        PlaybackDiagnosticsLog.clear()
        PlaybackDiagnosticsLog.info("MPV", "INIT", "Initialized Vulkan GPU-Next")
        PlaybackDiagnosticsLog.warn("EXO", "DROPPED_FRAMES", "Dropped 2 frames")
        PlaybackDiagnosticsLog.error("MPV", "ERROR", "Test error")

        val list = PlaybackDiagnosticsLog.entries.value
        assertEquals(3, list.size)
        assertEquals("ERROR", list[0].level)
        assertEquals("INFO", list[2].level)
        val report = PlaybackDiagnosticsLog.formatReport()
        assertTrue(report.contains("Initialized Vulkan GPU-Next"))
        assertTrue(report.contains("DROPPED_FRAMES"))

        PlaybackDiagnosticsLog.clear()
        assertTrue(PlaybackDiagnosticsLog.entries.value.isEmpty())
    }

    @Test
    fun smartCollectionsQualityFiltersWatchStatsAndLibraryHealthWorkCleanly() {
        val dayMs = 24L * 3600L * 1000L
        val nowMs = 1_750_000_000_000L
        val entries = listOf(
            SmartMediaEntry(
                id = "vid1",
                title = "Unwatched Movie 4K",
                progress = 0f,
                isWatched = false,
                durationMs = 5_400_000L, // 90 min (LONG)
                sizeBytes = 4_000_000_000L,
                dateAddedTimestamp = nowMs - 2L * dayMs,
                resolutionLabel = "4K",
                fileNameWithExt = "Movie.2160p.mkv"
            ),
            SmartMediaEntry(
                id = "vid2",
                title = "Resume Episode",
                progress = 0.45f,
                isWatched = false,
                durationMs = 1_400_000L, // ~23 min (LONG)
                sizeBytes = 600_000_000L,
                dateAddedTimestamp = nowMs - 3L * dayMs,
                resolutionLabel = "1080p",
                fileNameWithExt = "Show.1080p.mp4"
            ),
            SmartMediaEntry(
                id = "vid3",
                title = "Completed Short Clip",
                progress = 1f,
                isWatched = true,
                durationMs = 90_000L, // 1.5 min (SHORT)
                sizeBytes = 35_000_000L,
                dateAddedTimestamp = nowMs - 40L * dayMs,
                resolutionLabel = "720p",
                fileNameWithExt = "Clip.720p.mp4"
            ),
            SmartMediaEntry(
                id = "vid4",
                title = "Zero Duration Dup",
                progress = 0f,
                isWatched = false,
                durationMs = 0L,
                sizeBytes = 35_000_000L,
                dateAddedTimestamp = nowMs - 40L * dayMs,
                resolutionLabel = "",
                fileNameWithExt = "Clip.720p.mp4"
            )
        )
        val history = listOf(
            SmartHistoryEntry("vid2", "Resume Episode", "video/mp4", 630_000L, 1_400_000L, nowMs - dayMs, false),
            SmartHistoryEntry("file:///missing/nonexistent_movie.mkv", "Ghost", "video/x-matroska", 50_000L, 500_000L, nowMs - 2L * dayMs, true)
        )

        assertEquals(listOf("vid1", "vid4"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.UNWATCHED, entries, history, nowMs))
        assertEquals(listOf("vid2"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.PARTIALLY_WATCHED, entries, history, nowMs))
        assertEquals(listOf("vid3"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.COMPLETED, entries, history, nowMs))
        assertEquals(listOf("vid1", "vid2"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.LONG_VIDEOS, entries, history, nowMs))
        assertEquals(listOf("vid3"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.SHORT_VIDEOS, entries, history, nowMs))
        assertEquals(listOf("vid2"), SmartCollectionsAndStats.resolveSmartCollectionIds(SmartCollectionType.RECENTLY_PLAYED, entries, history, nowMs))

        assertTrue(SmartCollectionsAndStats.matchesQualityFilter("4K", "Movie.2160p.mkv", MediaQualityFilterType.UHD_4K))
        assertFalse(SmartCollectionsAndStats.matchesQualityFilter("720p", "Clip.mp4", MediaQualityFilterType.UHD_4K))
        assertTrue(SmartCollectionsAndStats.matchesQualityFilter("1080p", "Show.mkv", MediaQualityFilterType.MKV))

        val stats = SmartCollectionsAndStats.computeWatchStatisticsFromRecords(history, nowMs = nowMs)
        assertEquals(2, stats.videosPlayedCount)
        assertEquals(1, stats.completedCount)
        assertEquals(630_000L + 500_000L, stats.totalWatchTimeMs)
        assertEquals(7, stats.dailyBreakdown.size)

        val health = SmartCollectionsAndStats.computeLibraryHealthFromRecords(entries, history) { false }
        assertEquals(4, health.totalIndexedVideos)
        assertEquals(1, health.missingFileUriStrings.size)
        assertEquals(1, health.zeroDurationCount)
        assertEquals(1, health.duplicatePathCount)
    }

    @Test
    fun introOutroManagerParsesEpisodesMatchesChaptersWithoutFalsePositivesAndParsesJson() {
        val ep = IntroOutroManager.parseEpisodeInfo("Solo.Leveling.S02E05.1080p.WEB-DL.mkv")
        assertEquals("Solo Leveling", ep.cleanTitle)
        assertEquals(2, ep.season)
        assertEquals(5, ep.episode)

        val movie = IntroOutroManager.parseEpisodeInfo("Inception (2010) 2160p.mkv")
        assertEquals(2010, movie.year)

        val chapters = listOf(
            VideoChapterItem("Opening Theme", 12_000L, 95_000L),
            VideoChapterItem("Act 1: Hope and Developer Struggle", 95_000L, 900_000L),
            VideoChapterItem("Ending Credits", 900_000L, 990_000L)
        )
        val detected = IntroOutroManager.detectFromChapters(
            chapters = chapters,
            totalDurationMs = 990_000L,
            customIntroCsv = "",
            customOutroCsv = ""
        )
        // Must match Opening and Ending, and MUST NOT false-positive "op" inside "Hope" or "ed" inside word!
        assertEquals(2, detected.size)
        assertEquals(IntroSegmentKind.INTRO, detected[0].kind)
        assertEquals(IntroSegmentKind.OUTRO, detected[1].kind)
        assertTrue(detected[0].contains(45_000L))
        assertFalse(detected[0].contains(150_000L))

        val json = """{"intro":{"start_sec":10.5,"end_sec":95.0},"outro":{"start_sec":1200.0,"end_sec":1290.0}}"""
        val parsed = IntroOutroManager.parseIntroJsonSegments(json, "TheIntroDB")
        assertEquals(2, parsed.size)
        assertEquals(10_500L, parsed[0].startMs)
        assertEquals(IntroSegmentKind.OUTRO, parsed[1].kind)
    }

    @Test
    fun onlineSubtitleClientParsesWyzieAndSubDlJsonCleanly() {
        val wyzieRaw = """
            [
              {"id":"w1","url":"https://sub.wyzie.io/dl/1.srt","language":"en","display":"English","format":"srt","fileName":"Show.S01E01.srt","source":"opensubtitles","isHearingImpaired":true,"downloadCount":420},
              {"id":"w2","url":"","language":"en"}
            ]
        """.trimIndent()
        val wyzieParsed = OnlineSubtitleClient.parseWyzieJson(wyzieRaw, "en")
        assertEquals(1, wyzieParsed.size)
        assertEquals("Show.S01E01.srt", wyzieParsed[0].title)
        assertTrue(wyzieParsed[0].isHearingImpaired)

        val subdlRaw = """
            {"status":true,"subtitles":[{"sd_id":"99","url":"/subtitle/99.srt","lang":"en","language":"English","release_name":"Release.1080p","hi":false}]}
        """.trimIndent()
        val subdlParsed = OnlineSubtitleClient.parseSubDlJson(subdlRaw)
        assertEquals(1, subdlParsed.size)
        assertEquals("https://dl.subdl.com/subtitle/99.srt", subdlParsed[0].downloadUrl)
    }

    @Test
    fun mediaServerClientsParseJellyfinNavidromeAndAudiobookshelfPayloadsCleanly() {
        assertEquals("http://media.local:8096", MediaServerUrlUtils.normalizeBaseUrl("media.local:8096/"))
        assertEquals("https://media.local:8096", MediaServerUrlUtils.normalizeBaseUrl("https://media.local:8096/"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", MediaServerUrlUtils.md5Hex("hello"))

        val jfJson = """{"Items":[{"Id":"m1","Name":"Big Buck Bunny","Type":"Movie","IsFolder":false,"RunTimeTicks":600000000,"UserData":{"PlaybackPositionTicks":120000000}}]}"""
        val jfItems = JellyfinClient.parseItemsJson(jfJson, "https://jf.local", "tok1")
        assertEquals(1, jfItems.size)
        assertEquals(60_000L, jfItems[0].runTimeMs)
        assertEquals(12_000L, jfItems[0].resumePositionMs)
        assertTrue(jfItems[0].streamUrl!!.contains("/Videos/m1/stream"))

        val ndAlbumJson = """{"subsonic-response":{"status":"ok","albumList2":{"album":[{"id":"a1","name":"Synthwave","artist":"Artist X","songCount":10,"duration":2400}]}}}"""
        val albums = NavidromeClient.parseAlbumsJson(ndAlbumJson, "https://nd.local", "u", "p")
        assertEquals(1, albums.size)
        assertEquals("Synthwave", albums[0].name)

        val absJson = """{"results":[{"id":"b1","media":{"duration":3600.0,"metadata":{"title":"Dune","authorName":"Frank Herbert"},"chapters":[{"title":"Chapter 1","start":0.0,"end":1200.0}],"audioFiles":[{"ino":"f1"}]},"userMediaProgress":{"currentTime":450.0}}]}"""
        val books = AudiobookshelfClient.parseBooksJson(absJson, "https://abs.local", "jwt1")
        assertEquals(1, books.size)
        assertEquals("Dune", books[0].title)
        assertEquals(450_000L, books[0].currentTimeMs)
        assertEquals(1, books[0].chapters.size)
    }

    @Test
    fun encryptedCredentialStoreAndDeepSearchCoverNewFeatures() {
        EncryptedCredentialStore.clearAll(null)
        EncryptedCredentialStore.putSecret(null, "k1", "secret_val")
        assertEquals("secret_val", EncryptedCredentialStore.getSecret(null, "k1"))
        EncryptedCredentialStore.removeSecret(null, "k1")
        assertEquals("", EncryptedCredentialStore.getSecret(null, "k1"))

        assertTrue(DeepSettingsSearchIndex.search("mpv.conf").any { it.page == SettingsDestinationPage.ADVANCED })
        assertTrue(DeepSettingsSearchIndex.search("intro skip").any { it.page == SettingsDestinationPage.PLAYBACK })
        assertTrue(DeepSettingsSearchIndex.search("online subtitle").any { it.page == SettingsDestinationPage.SUBTITLES })
        assertTrue(DeepSettingsSearchIndex.search("saf folder").any { it.page == SettingsDestinationPage.MEDIA_LIBRARY })
        assertTrue(DeepSettingsSearchIndex.search("visualizer").any { it.page == SettingsDestinationPage.AUDIO })
        assertTrue(DeepSettingsSearchIndex.search("jellyfin").any { it.page == SettingsDestinationPage.NAVIGATION })
    }

    @Test
    fun autoFormatOrientationAndCinemaHudDefaultsWorkCleanly() {
        assertEquals(1080 to 1920, PlayerControlCatalog.resolveDisplayDimensions(1080, 1920, 0))
        assertEquals(1080 to 1920, PlayerControlCatalog.resolveDisplayDimensions(1920, 1080, 90))
        assertEquals(1080 to 1920, PlayerControlCatalog.resolveDisplayDimensions(1920, 1080, 270))
        assertEquals(1080 to 1920, PlayerControlCatalog.resolveDisplayDimensions(1920, 1080, -90))
        assertEquals(1920 to 1080, PlayerControlCatalog.resolveDisplayDimensions(1920, 1080, 180))
        assertTrue(PlayerControlCatalog.isPortraitVideo(1080, 1920, 0))
        assertTrue(PlayerControlCatalog.isPortraitVideo(1920, 1080, 90))
        assertFalse(PlayerControlCatalog.isPortraitVideo(1920, 1080, 0))
        assertFalse(PlayerControlCatalog.isPortraitVideo(1080, 1080, 0))

        AppSettings.resetHudLayoutToDefault()
        assertTrue(AppSettings.hidePlayerButtonsBg.value)
        assertFalse(AppSettings.showSeekbarOuterContainer.value)
        assertEquals("BELOW_SEEKBAR", AppSettings.portraitControlsPosition.value)
        assertEquals("NORMAL", AppSettings.seekbarStyle.value)
        val dupes = PlayerControlCatalog.findDuplicateTokens(
            listOf(
                AppSettings.topRightHudControls.value,
                AppSettings.bottomLeftHudControls.value,
                AppSettings.bottomRightHudControls.value
            )
        )
        assertTrue(dupes.isEmpty())
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val map = linkedMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
            if (key != null) pending[key] = values?.toSet()
        }
        override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
        override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
        override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
        override fun remove(key: String?) = apply { if (key != null) pending[key] = REMOVE_SENTINEL }
        override fun clear() = apply { clearAll = true }
        override fun commit(): Boolean {
            if (clearAll) map.clear()
            pending.forEach { (k, v) -> if (v === REMOVE_SENTINEL) map.remove(k) else map[k] = v }
            return true
        }
        override fun apply() { commit() }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private companion object {
        val REMOVE_SENTINEL = Any()
    }
}
