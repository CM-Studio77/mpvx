# Open-Source Notices

**MPVX Player** is free software distributed under the **GNU General Public License v3.0** (`LICENSE`).
It bundles the open-source media engines, decoders, shaders, and runtime libraries listed below under their respective licences.

Full licence texts live in [`licenses/`](licenses/) and are packaged inside the application (Settings → About → Open-Source Licenses).

---

## Core Engines, Codecs & Subtitle Renderers

| Component | Licence |
|---|---|
| **libmpv & mpv-android JNI** (`mpvlib.aar`) | MIT (JNI bindings); libmpv GNU LGPL v2.1+ / GPL v2+ |
| **FFmpeg** (via libmpv & Media3 FFmpeg Decoder Extension) | GNU LGPL v2.1+ / GNU GPL v2+ (Apache-2.0 Media3 wrapper) |
| **AndroidX Media3 / ExoPlayer & Decoder Extensions** (`dav1d` AV1, IAMF, MPEGH) | Apache-2.0 / BSD-2-Clause (`dav1d`) |
| **libass / ass-kt / ass-media** | ISC (`libass`) / Apache-2.0 (`ass-kt`, `ass-media`) |
| **Anime4K GLSL Shaders** | MIT |

---

## UI, Storage & Network Libraries

| Component | Licence |
|---|---|
| **Jetpack Compose, Material3 Expressive, AndroidX Core, AppCompat, Lifecycle, Preference, RecyclerView** | Apache-2.0 |
| **Material Components for Android** | Apache-2.0 |
| **Reorderable** | Apache-2.0 |
| **Coil** (`coil-compose`, `coil-video`) | Apache-2.0 |
| **OkHttp** | Apache-2.0 |
| **SMBJ & Apache Commons Net** (SMB / FTP / WebDAV) | Apache-2.0 |
| **JSch & NanoHTTPD** (SFTP & local streaming bridge) | BSD-3-Clause |
| **ZXing Core & chardet4j** | Apache-2.0 |
