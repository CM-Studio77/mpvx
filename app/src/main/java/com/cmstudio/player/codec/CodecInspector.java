package com.cmstudio.player.codec;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CodecInspector {

    private CodecInspector() {}

    public static class KeyCodecSummary {
        public final String formatName;
        public final String mimeType;
        public boolean hasHardware;
        public boolean hasSoftware;
        public String defaultDecoderName;
        public String maxResolution;
        public boolean isHdrSupported;
        public boolean is10BitSupported;

        public KeyCodecSummary(String formatName, String mimeType) {
            this.formatName = formatName;
            this.mimeType = mimeType;
        }
    }

    public static class InspectionResult {
        public final List<CodecCapabilitiesInfo> codecs = new ArrayList<>();
        public final List<KeyCodecSummary> keySummaries = new ArrayList<>();
        public int totalCount = 0;
        public int hardwareCount = 0;
        public int softwareCount = 0;
        public int videoCount = 0;
        public int audioCount = 0;
    }

    public static boolean isHardwareDecoder(MediaCodecInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated();
        }
        final String name = info.getName().toLowerCase(Locale.US);
        if (name.startsWith("omx.google.") || name.startsWith("c2.android.") ||
            name.startsWith("omx.ffmpeg.") || name.contains(".sw.") || name.endsWith(".sw")) {
            return false;
        }
        return name.startsWith("omx.") || name.startsWith("c2.");
    }

    public static InspectionResult inspect(Context context) {
        final InspectionResult result = new InspectionResult();
        final Map<String, KeyCodecSummary> keyMap = new LinkedHashMap<>();

        keyMap.put("video/hevc", new KeyCodecSummary("HEVC (H.265)", "video/hevc"));
        keyMap.put("video/av01", new KeyCodecSummary("AV1", "video/av01"));
        keyMap.put("video/avc", new KeyCodecSummary("AVC (H.264)", "video/avc"));
        keyMap.put("video/x-vnd.on2.vp9", new KeyCodecSummary("VP9", "video/x-vnd.on2.vp9"));
        keyMap.put("video/dolby-vision", new KeyCodecSummary("Dolby Vision", "video/dolby-vision"));
        keyMap.put("video/x-vnd.on2.vp8", new KeyCodecSummary("VP8", "video/x-vnd.on2.vp8"));
        keyMap.put("audio/opus", new KeyCodecSummary("Opus", "audio/opus"));
        keyMap.put("audio/flac", new KeyCodecSummary("FLAC", "audio/flac"));
        keyMap.put("audio/mp4a-latm", new KeyCodecSummary("AAC", "audio/mp4a-latm"));
        keyMap.put("audio/ac3", new KeyCodecSummary("Dolby Digital (AC-3)", "audio/ac3"));
        keyMap.put("audio/eac3", new KeyCodecSummary("Dolby Digital Plus (E-AC-3)", "audio/eac3"));

        try {
            final MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            final MediaCodecInfo[] infos = codecList.getCodecInfos();

            for (MediaCodecInfo info : infos) {
                if (info == null || info.isEncoder()) {
                    continue; // We only inspect playback decoders
                }

                final boolean isHw = isHardwareDecoder(info);
                final String canonicalName = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ? info.getCanonicalName()
                        : info.getName();

                for (String mime : info.getSupportedTypes()) {
                    if (mime == null) continue;
                    final boolean isVideo = mime.startsWith("video/");
                    final boolean isAudio = mime.startsWith("audio/");
                    if (!isVideo && !isAudio) continue;

                    final String formatName = getFormatName(mime);
                    final CodecCapabilitiesInfo item = new CodecCapabilitiesInfo(
                            info.getName(), canonicalName, isHw, isVideo, mime, formatName);

                    try {
                        final MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
                        if (caps != null) {
                            inspectCapabilities(caps, item, isVideo);
                        }
                    } catch (Exception ignored) {}

                    result.codecs.add(item);
                    result.totalCount++;
                    if (isHw) result.hardwareCount++; else result.softwareCount++;
                    if (isVideo) result.videoCount++; else result.audioCount++;

                    final KeyCodecSummary summary = keyMap.get(mime.toLowerCase(Locale.US));
                    if (summary != null) {
                        if (isHw) {
                            summary.hasHardware = true;
                            if (summary.defaultDecoderName == null || !summary.hasHardware) {
                                summary.defaultDecoderName = info.getName();
                            }
                        } else {
                            summary.hasSoftware = true;
                            if (summary.defaultDecoderName == null) {
                                summary.defaultDecoderName = info.getName();
                            }
                        }
                        if (item.maxResolution != null && (summary.maxResolution == null || isHw)) {
                            summary.maxResolution = item.maxResolution;
                        }
                        if (item.isHdrSupported) summary.isHdrSupported = true;
                        if (item.is10BitSupported) summary.is10BitSupported = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        result.keySummaries.addAll(keyMap.values());
        Collections.sort(result.codecs, (a, b) -> {
            if (a.isHardware != b.isHardware) {
                return a.isHardware ? -1 : 1; // Hardware decoders first
            }
            if (a.isVideo != b.isVideo) {
                return a.isVideo ? -1 : 1; // Video decoders before audio
            }
            return a.formatName.compareToIgnoreCase(b.formatName);
        });

        return result;
    }

    private static void inspectCapabilities(MediaCodecInfo.CodecCapabilities caps, CodecCapabilitiesInfo item, boolean isVideo) {
        if (caps == null) return;

        // Hardware Features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback)) {
                item.features.add("Adaptive Resizing");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)) {
                item.features.add("Direct Hardware Tunneling");
            }
            if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)) {
                item.features.add("Hardware DRM (L1 Secure)");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)) {
                item.features.add("Ultra-Low Latency");
            }
        }

        if (isVideo) {
            final MediaCodecInfo.VideoCapabilities vc = caps.getVideoCapabilities();
            if (vc != null) {
                try {
                    final Range<Integer> widths = vc.getSupportedWidths();
                    final Range<Integer> heights = vc.getSupportedHeights();
                    final Range<Integer> fps = vc.getSupportedFrameRates();
                    final int maxW = widths != null ? widths.getUpper() : 0;
                    final int maxH = heights != null ? heights.getUpper() : 0;
                    final int maxFps = fps != null ? fps.getUpper() : 0;

                    if (maxW > 0 && maxH > 0) {
                        item.maxResolution = maxW + "x" + maxH + (maxFps > 0 ? " @" + maxFps + "fps" : "");
                        item.minResolution = widths.getLower() + "x" + heights.getLower();
                        item.maxFrameRate = maxFps;
                    }
                    final Range<Integer> bitrates = vc.getBitrateRange();
                    if (bitrates != null) {
                        item.bitrateRange = formatBitrate(bitrates.getLower()) + " - " + formatBitrate(bitrates.getUpper());
                    }
                    item.alignment = vc.getWidthAlignment() + "x" + vc.getHeightAlignment();
                } catch (Exception ignored) {}
            }

            // Profiles & Levels
            if (caps.profileLevels != null) {
                for (MediaCodecInfo.CodecProfileLevel pl : caps.profileLevels) {
                    final String profName = getProfileName(item.mimeType, pl.profile);
                    if (profName != null && !item.profilesAndLevels.contains(profName)) {
                        item.profilesAndLevels.add(profName);
                    }
                    if (isHdrOr10BitProfile(item.mimeType, pl.profile)) {
                        item.isHdrSupported = true;
                        item.is10BitSupported = true;
                    }
                }
            }

            // Color Formats
            if (caps.colorFormats != null) {
                for (int cf : caps.colorFormats) {
                    final String colorName = getColorFormatName(cf);
                    if (!item.colorFormats.contains(colorName)) {
                        item.colorFormats.add(colorName);
                    }
                    if (cf == 0x7F420C00 || cf == 0x7F420000 || cf == 54 /* P010 */) {
                        item.is10BitSupported = true;
                    }
                }
            }
        } else {
            final MediaCodecInfo.AudioCapabilities ac = caps.getAudioCapabilities();
            if (ac != null) {
                try {
                    item.maxChannels = ac.getMaxInputChannelCount();
                    final int[] rates = ac.getSupportedSampleRates();
                    if (rates != null) {
                        for (int r : rates) {
                            item.sampleRates.add(String.format(Locale.US, "%.1f kHz", r / 1000.0));
                        }
                    }
                    final Range<Integer> bitrates = ac.getBitrateRange();
                    if (bitrates != null) {
                        item.bitrateRange = formatBitrate(bitrates.getLower()) + " - " + formatBitrate(bitrates.getUpper());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static String getFormatName(String mime) {
        if (mime == null) return "Unknown";
        final String lower = mime.toLowerCase(Locale.US);
        switch (lower) {
            case "video/hevc": return "HEVC (H.265)";
            case "video/av01": return "AV1";
            case "video/avc": return "AVC (H.264)";
            case "video/x-vnd.on2.vp9": return "VP9";
            case "video/dolby-vision": return "Dolby Vision";
            case "video/x-vnd.on2.vp8": return "VP8";
            case "video/mp4v-es": return "MPEG-4 Video";
            case "video/3gpp": return "H.263";
            case "audio/opus": return "Opus Audio";
            case "audio/flac": return "FLAC Lossless";
            case "audio/mp4a-latm": return "AAC Audio";
            case "audio/ac3": return "Dolby Digital (AC-3)";
            case "audio/eac3": return "Dolby Digital Plus (E-AC-3)";
            case "audio/vorbis": return "Ogg Vorbis";
            case "audio/mpeg": return "MP3 Audio";
            default:
                if (lower.startsWith("video/")) return lower.substring(6).toUpperCase(Locale.US);
                if (lower.startsWith("audio/")) return lower.substring(6).toUpperCase(Locale.US);
                return mime;
        }
    }

    public static String formatBitrate(int bps) {
        if (bps >= 1_000_000) {
            return String.format(Locale.US, "%.1f Mbps", bps / 1_000_000.0);
        } else if (bps >= 1_000) {
            return (bps / 1000) + " Kbps";
        }
        return bps + " bps";
    }

    private static boolean isHdrOr10BitProfile(String mime, int profile) {
        if (mime == null) return false;
        final String lower = mime.toLowerCase(Locale.US);
        if (lower.equals("video/hevc")) {
            return profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                   profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                   profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus;
        }
        if (lower.equals("video/av01")) {
            return profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10 ||
                   profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 ||
                   profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus;
        }
        if (lower.equals("video/x-vnd.on2.vp9")) {
            return profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2 ||
                   profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR ||
                   profile == MediaCodecInfo.CodecProfileLevel.VP9Profile3 ||
                   profile == MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR;
        }
        if (lower.equals("video/dolby-vision")) {
            return true;
        }
        return false;
    }

    private static String getProfileName(String mime, int profile) {
        if (mime == null) return "Profile " + profile;
        final String lower = mime.toLowerCase(Locale.US);
        if (lower.equals("video/hevc")) {
            switch (profile) {
                case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain: return "Main (8-bit)";
                case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10: return "Main 10 (10-bit)";
                case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10: return "Main 10 HDR10";
                case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus: return "Main 10 HDR10+";
                default: return "Profile " + profile;
            }
        }
        if (lower.equals("video/avc")) {
            switch (profile) {
                case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline: return "Baseline";
                case MediaCodecInfo.CodecProfileLevel.AVCProfileMain: return "Main";
                case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh: return "High";
                case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10: return "High 10 (10-bit)";
                default: return "Profile " + profile;
            }
        }
        if (lower.equals("video/av01")) {
            switch (profile) {
                case MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8: return "Main 8-bit";
                case MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10: return "Main 10-bit";
                case MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10: return "Main 10 HDR10";
                case MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus: return "Main 10 HDR10+";
                default: return "Profile " + profile;
            }
        }
        if (lower.equals("video/x-vnd.on2.vp9")) {
            switch (profile) {
                case MediaCodecInfo.CodecProfileLevel.VP9Profile0: return "Profile 0 (8-bit)";
                case MediaCodecInfo.CodecProfileLevel.VP9Profile2: return "Profile 2 (10-bit)";
                case MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR: return "Profile 2 HDR10";
                default: return "Profile " + profile;
            }
        }
        return "Profile " + profile;
    }

    private static String getColorFormatName(int cf) {
        switch (cf) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: return "YUV420 Planar";
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: return "YUV420 Semi-Planar (NV12)";
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface: return "Direct Surface Rendering";
            case 54: return "YUV P010 (10-bit HDR)";
            case 0x7F420000: return "QTI 10-bit Compressed";
            case 0x7FA00000: return "TIFormat 10-bit";
            default: return "Format 0x" + Integer.toHexString(cf).toUpperCase(Locale.US);
        }
    }
}
