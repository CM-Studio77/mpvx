package com.cmstudio.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.PlaybackException;

public final class PlaybackError {

    private PlaybackError() {
    }

    @NonNull
    public static String plainly(final Context context, @Nullable final PlaybackException error) {
        if (error == null) {
            return context.getString(R.string.error_unknown);
        }
        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
                return context.getString(R.string.error_network);
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
                return context.getString(R.string.error_http);
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
                return context.getString(R.string.error_missing);
            case PlaybackException.ERROR_CODE_IO_NO_PERMISSION:
                return context.getString(R.string.error_permission);
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED:
                return context.getString(R.string.error_codec);
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED:
                return context.getString(R.string.error_damaged);
            case PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED:
                return context.getString(R.string.error_drm);
            default:
                return context.getString(R.string.error_unknown);
        }
    }

    @NonNull
    public static String details(@NonNull final Context context, @Nullable final PlaybackException error,
            @NonNull final String engine, @Nullable final String uri) {
        final StringBuilder text = new StringBuilder();
        text.append("MPVX ").append(BuildConfig.VERSION_NAME).append('\n')
                .append("Android ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                .append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                .append("Engine: ").append(engine).append('\n');
        // scheme and host only: the full URL can carry a token
        if (uri != null) {
            final Uri parsed = Uri.parse(uri);
            text.append("Source: ").append(parsed.getScheme()).append("://")
                    .append(parsed.getHost() == null ? "local" : parsed.getHost()).append('\n');
        }
        if (error != null) {
            text.append("Code: ").append(error.getErrorCodeName())
                    .append(" (").append(error.errorCode).append(")\n")
                    .append("Message: ").append(error.getMessage()).append('\n');
            if (error.getCause() != null) {
                text.append("Cause: ").append(error.getCause()).append('\n');
            }
        }
        return text.toString();
    }

    public static void share(final Activity activity, final String details) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "MPVX playback error");
        intent.putExtra(Intent.EXTRA_TEXT, details);
        try {
            activity.startActivity(Intent.createChooser(intent,
                    activity.getString(R.string.error_share)));
        } catch (Exception ignored) {
        }
    }
}
