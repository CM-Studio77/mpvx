package com.cmstudio.player.codec;

import java.util.ArrayList;
import java.util.List;

public class CodecCapabilitiesInfo {
    public final String name;
    public final String canonicalName;
    public final boolean isHardware;
    public final boolean isVideo;
    public final String mimeType;
    public final String formatName;
    public String maxResolution;
    public String minResolution;
    public Integer maxFrameRate;
    public String bitrateRange;
    public boolean isHdrSupported;
    public boolean is10BitSupported;
    public String alignment;
    public Integer maxChannels;
    public final List<String> profilesAndLevels = new ArrayList<>();
    public final List<String> colorFormats = new ArrayList<>();
    public final List<String> features = new ArrayList<>();
    public final List<String> sampleRates = new ArrayList<>();

    public CodecCapabilitiesInfo(String name, String canonicalName, boolean isHardware, boolean isVideo,
                                 String mimeType, String formatName) {
        this.name = name;
        this.canonicalName = canonicalName;
        this.isHardware = isHardware;
        this.isVideo = isVideo;
        this.mimeType = mimeType;
        this.formatName = formatName;
    }
}
