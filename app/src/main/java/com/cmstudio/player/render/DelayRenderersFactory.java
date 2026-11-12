package com.cmstudio.player.render;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ForwardingRenderer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.audio.ForwardingAudioSink;
import androidx.media3.exoplayer.text.TextOutput;

import com.cmstudio.player.ui.player.ChannelMixMath;
import com.cmstudio.player.ui.player.SubtitleTimingMath;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class DelayRenderersFactory extends DefaultRenderersFactory {

    private final AtomicInteger subtitleDelayMs;
    private final AtomicInteger subtitleSpeedMilli;
    private final AtomicInteger audioDelayMs;
    private final AtomicReference<String> channelMode;

    public DelayRenderersFactory(Context context, AtomicInteger subtitleDelayMs,
                                 AtomicInteger audioDelayMs) {
        this(context, subtitleDelayMs, new AtomicInteger(1000), audioDelayMs,
                new AtomicReference<>(ChannelMixMath.MODE_AUTO));
    }

    public DelayRenderersFactory(Context context,
                                 AtomicInteger subtitleDelayMs,
                                 AtomicInteger subtitleSpeedMilli,
                                 AtomicInteger audioDelayMs,
                                 AtomicReference<String> channelMode) {
        super(context);
        this.subtitleDelayMs = subtitleDelayMs;
        this.subtitleSpeedMilli = subtitleSpeedMilli;
        this.audioDelayMs = audioDelayMs;
        this.channelMode = channelMode;
    }

    // a parse-time shift can't move embedded cues earlier: their times start at 0
    @Override
    protected void buildTextRenderers(@NonNull Context context, @NonNull TextOutput output, @NonNull Looper outputLooper, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
        ArrayList<Renderer> textRenderers = new ArrayList<>();
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, textRenderers);
        for (Renderer renderer : textRenderers) {
            if (renderer.getTrackType() == C.TRACK_TYPE_TEXT) {
                out.add(new SubtitleDelayRenderer(renderer, subtitleDelayMs, subtitleSpeedMilli));
            } else {
                out.add(renderer);
            }
        }
    }

    // shifts the audio clock and applies live channel matrix mixing (Mono/Stereo/Reverse)
    @Override
    protected AudioSink buildAudioSink(@NonNull Context context, boolean enableFloatOutput,
                                       boolean enableAudioTrackPlaybackParams) {
        final AudioSink baseSink = new DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(new AudioProcessor[] {
                        new ChannelModeAudioProcessor(channelMode)
                })
                .build();
        return new AudioDelaySink(baseSink, audioDelayMs);
    }

    private static final class SubtitleDelayRenderer extends ForwardingRenderer {

        private final AtomicInteger delayMs;
        private final AtomicInteger speedMilli;

        private SubtitleDelayRenderer(Renderer renderer, AtomicInteger delayMs, AtomicInteger speedMilli) {
            super(renderer);
            this.delayMs = delayMs;
            this.speedMilli = speedMilli;
        }

        private long adjustedUs(long positionUs) {
            return SubtitleTimingMath.INSTANCE.adjustedPositionUs(
                    positionUs, delayMs.get(), speedMilli.get());
        }

        @Override
        public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
            // an earlier position delays the cues, a later one advances them; speed scales progression
            super.render(adjustedUs(positionUs), elapsedRealtimeUs);
        }

        @Override
        public long getDurationToProgressUs(long positionUs, long elapsedRealtimeUs) {
            return super.getDurationToProgressUs(adjustedUs(positionUs), elapsedRealtimeUs);
        }

    }

    private static final class ChannelModeAudioProcessor extends BaseAudioProcessor {

        private final AtomicReference<String> channelMode;

        private ChannelModeAudioProcessor(AtomicReference<String> channelMode) {
            this.channelMode = channelMode;
        }

        @Override
        @NonNull
        protected AudioFormat onConfigure(@NonNull AudioFormat inputAudioFormat) {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount < 2) {
                return AudioFormat.NOT_SET;
            }
            return inputAudioFormat;
        }

        @Override
        public void queueInput(@NonNull ByteBuffer inputBuffer) {
            final int remaining = inputBuffer.remaining();
            if (remaining <= 0) {
                return;
            }
            final ByteBuffer out = replaceOutputBuffer(remaining);
            final String mode = channelMode.get();
            ChannelMixMath.INSTANCE.processLittleEndianBuffer(
                    inputBuffer, out, inputAudioFormat.channelCount, mode);
            out.flip();
        }
    }

    private static final class AudioDelaySink extends ForwardingAudioSink {

        private final AtomicInteger delayMs;

        private AudioDelaySink(AudioSink sink, AtomicInteger delayMs) {
            super(sink);
            this.delayMs = delayMs;
        }

        @Override
        public long getCurrentPositionUs(boolean sourceEnded) {
            final long positionUs = super.getCurrentPositionUs(sourceEnded);
            if (positionUs == AudioSink.CURRENT_POSITION_NOT_SET) {
                return positionUs;
            }
            return Math.max(0, positionUs + delayMs.get() * 1000L);
        }

    }

}

