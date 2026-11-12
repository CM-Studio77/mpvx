# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in **MPVX Player**, please report it responsibly by opening a confidential report or contacting the project maintainers via GitHub.

Reports are reviewed and acted upon as quickly as possible.

## Security Scope

The player operates with strict privacy principles: it reads local and user-selected storage, and connects to external networks only for explicitly configured features (online subtitles, Jellyfin, Navidrome, Audiobookshelf, and SMB/FTP/SFTP/WebDAV shares) using user-supplied keys and credentials stored with Android Keystore encryption.

Key security areas:
- **Phone Setup Service**: While active, the local web setup server operates strictly on the local network protected by PIN authorization.
- **Credential Storage**: API keys and network credentials remain private and are excluded from diagnostic exports unless explicitly requested.
- **Malicious Containers & Codecs**: Robust parsing to ensure malformed media files or subtitle streams do not execute arbitrary code.
- **Network Integrity**: Direct communication only with the endpoints specified by the user.

## Supported Versions

Security fixes are applied directly to the latest release of **MPVX Player**.
