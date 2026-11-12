<div align="center">

# MPVX Player

### Dual-Engine Material 3 Android Video & Music Player

**MPVX Player** (`com.cmstudio.player`) is a modern Android video and audio player combining **native libmpv** and **AndroidX Media3 (ExoPlayer)** with Jetpack Compose Material 3 Expressive themes, customizable Player HUD layouts, ASS/SSA (`libass`) subtitles, LAN/Media Server streaming, and an integrated Music Studio.

[![Android](https://img.shields.io/badge/Android-7.1%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Engine](https://img.shields.io/badge/Engines-libmpv%20%2B%20Media3%20ExoPlayer-00B4D8?style=flat-square)](#)
[![Licence](https://img.shields.io/badge/Licence-GPL--3.0-8A8A8A?style=flat-square)](LICENSE)

</div>

---

## Features

- **Dual Playback Engines (`libmpv` + `Media3 ExoPlayer`)**: Automatic engine selection with per-video manual engine switching (`Auto`, `libmpv`, `ExoPlayer`), Vulkan / `gpu-next` / OpenGL ES video outputs, 10-bit HDR tone-mapping, and Anime4K v4.x GLSL shaders.
- **Customizable Player HUD & Gestures**: Visual drag-and-drop Player Control Layout Editor across landscape & portrait slots, 9 seekbar styles, edge quick-tools drawer, double-tap seek intervals, pinch-to-zoom/pan, A-B loop, frame stepping, PiP, and snapshot capture.
- **Subtitles & Audio Pipeline**: Embedded & external ASS/SSA (`ass-media` + `libass`), SRT, VTT, TTML, and SAMI subtitles, dual subtitles, online subtitle lookup (Wyzie & optional SubDL key), live delay/speed/edge styling, 10-band Equalizer, loudness boost, pitch correction, and chapter / intro-outro skip markers.
- **Library, Music Studio & Privacy Vault**: Fast MediaStore scanner with List/Grid/Compact Grid/Folder/Tree layouts, quality filters (`4K`, `1080p`, `720p`, `MKV`, `MP4`), Smart Collections, watch statistics, PIN-protected Privacy Vault, and Music Studio (Songs, Albums, Artists, Playlists, Folders, Wave Visualizer, Sleep Timer).
- **Network Shares & Media Servers**: Browse and stream from SMB, FTP, SFTP, WebDAV, HTTP directories, M3U playlists, Jellyfin, Navidrome (Subsonic), and Audiobookshelf with Android Keystore credential encryption.
- **43+ Material 3 Themes & Safe `mpv.conf`**: Built-in light, dark, and AMOLED pure black themes, custom live theme palette creator, optional Glass Effect toggle, 135+ searchable settings, JSON config backup/restore, and validated `mpv.conf` editor.

## Building

1. Copy `keystore.properties.example` to `keystore.properties` and configure your release keystore (optional for debug builds).
2. Build or run tests via Gradle:

```bash
./gradlew :app:testLatestUniversalDebugUnitTest
./gradlew :app:assembleLatestUniversalDebug
./gradlew :app:assembleLatestUniversalRelease
./gradlew :app:bundleLatestUniversalRelease
```

## License

MPVX Player is licensed under the **GNU General Public License v3.0** (`LICENSE`). Open-source component notices are listed in [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md) and [`licenses/`](licenses/).
