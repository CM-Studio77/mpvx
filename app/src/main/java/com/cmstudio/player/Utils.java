package com.cmstudio.player;

import static android.content.Context.UI_MODE_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;

import com.sigpwned.chardet4j.Chardet;
import com.sigpwned.chardet4j.io.DecodedInputStreamReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    public static final String TAG = "MPVXPlayer";

    public static final String FEATURE_FIRE_TV = "amazon.hardware.fire_tv";

    public static final String[] supportedExtensionsVideo = new String[] { "3gp", "m4v", "mkv", "mov", "mp4", "ts", "webm" };
    public static final String[] supportedExtensionsSubtitle = new String[] { "srt", "ssa", "ass", "vtt", "ttml", "dfxp", "xml",
            "smi", "sami", "mpl", "sub", "sup", "idx" };

    public static final String[] supportedMimeTypesVideo = new String[] {
            MimeTypes.VIDEO_MATROSKA,
            MimeTypes.VIDEO_MP4,
            MimeTypes.VIDEO_WEBM,
            "video/quicktime",
            "video/mp2ts", // .ts, but also incompatible .m2ts
            MimeTypes.VIDEO_H263,
            "video/x-m4v", // .m4v from remote storages
    };
    public static final String[] supportedMimeTypesSubtitle = new String[] {
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.TEXT_SSA,
            MimeTypes.TEXT_VTT,
            MimeTypes.APPLICATION_TTML,
            "text/*",
            "application/octet-stream"
    };

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static float pxToDp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    // no java.nio.file: it needs API 26, and catch (Exception) misses the Error on 25
    public static byte[] readFile(final File file) throws IOException {
        try (InputStream in = new java.io.FileInputStream(file)) {
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream((int) Math.max(0, file.length()));
            final byte[] buffer = new byte[8192];
            int got;
            while ((got = in.read(buffer)) > 0) {
                out.write(buffer, 0, got);
            }
            return out.toByteArray();
        }
    }

    public static boolean fileExists(final Context context, final Uri uri) {
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try {
                final InputStream inputStream = context.getContentResolver().openInputStream(uri);
                inputStream.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            String path;
            if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                path = uri.getPath();
            } else {
                path = uri.toString();
            }
            final File file = new File(path);
            return file.exists();
        }
    }

    public static String getFileName(Context context, Uri uri, boolean keepExtension) {
        if (uri == null) return "";
        String result = null;
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && context != null) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex > -1)
                            result = cursor.getString(columnIndex);
                    }
                }
            }
            if (result == null) {
                result = uri.getLastPathSegment();
                if (result == null) {
                    result = uri.getPath();
                }
                if (result != null) {
                    int cut = result.lastIndexOf('/');
                    if (cut != -1) {
                        result = result.substring(cut + 1);
                    }
                }
            }
            if (result != null && !keepExtension) {
                int dot = result.lastIndexOf(".");
                if (dot > 0) {
                    result = result.substring(0, dot);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return result != null ? result : "";
    }

    public static boolean isVolumeMax(final AudioManager audioManager) {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static boolean isVolumeMin(final AudioManager audioManager) {
        int min = Build.VERSION.SDK_INT >= 28 ? audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) : 0;
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == min;
    }

    public enum Orientation {
        LANDSCAPE(0, R.string.video_orientation_landscape),
        PORTRAIT(4, R.string.video_orientation_portrait),
        // SENSOR ignores the system rotation lock on purpose
        SENSOR(3, R.string.video_orientation_sensor);

        public final int value;
        public final int description;

        Orientation(int type, int description) {
            this.value = type;
            this.description = description;
        }

        // by saved value, not ordinal; retired modes map to their nearest equivalent
        public static Orientation fromValue(final int value) {
            switch (value) {
                case 4:
                    return PORTRAIT;
                case 1:
                case 3:
                    return SENSOR;
                default:
                    return LANDSCAPE;
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void setOrientation(Activity activity, Orientation orientation) {
        switch (orientation) {
            case PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                break;
            case SENSOR:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            case LANDSCAPE:
            default:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                break;
        }
    }

    public static Orientation getNextOrientation(Orientation orientation) {
        switch (orientation) {
            case LANDSCAPE:
                return Orientation.PORTRAIT;
            case PORTRAIT:
                return Orientation.SENSOR;
            case SENSOR:
            default:
                return Orientation.LANDSCAPE;
        }
    }

    public static boolean isRotated(final Format format) {
        return format.rotationDegrees == 90 || format.rotationDegrees == 270;
    }

    public static boolean isPortrait(final Format format) {
        if (isRotated(format)) {
            return format.width > format.height;
        } else {
            return format.height > format.width;
        }
    }

    public static Rational getRational(final Format format) {
        if (isRotated(format))
            return new Rational(format.height, format.width);
        else
            return new Rational(format.width, format.height);
    }

    public static String formatMilis(long time) {
        final int totalSeconds = Math.abs((int) time / 1000);
        final int seconds = totalSeconds % 60;
        final int minutes = totalSeconds % 3600 / 60;
        final int hours = totalSeconds / 3600;

        return (hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds));
    }

    public static String formatMilisSign(long time) {
        if (time > -1000 && time < 1000)
            return formatMilis(time);
        else
            return (time < 0 ? "−" : "+") + formatMilis(time);
    }

    public static void log(final String text) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, text);
        }
    }

    public static void setViewMargins(final View view, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        if (view == null) {
            return;
        }
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) layoutParams).setMargins(marginLeft, marginTop, marginRight, marginBottom);
            view.setLayoutParams(layoutParams);
        }
    }

    public static void setViewParams(final View view, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        if (view == null) {
            return;
        }
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        setViewMargins(view, marginLeft, marginTop, marginRight, marginBottom);
    }

    public static boolean isDeletable(final Context context, final Uri uri) {
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_FLAGS}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
                        if (columnIndex > -1) {
                            int flags = cursor.getInt(columnIndex);
                            return (flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) == DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
                        }
                    }
                }
            } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                if (Build.VERSION.SDK_INT >= 23) {
                    boolean hasPermission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
                    if (!hasPermission) {
                        return false;
                    }
                }
                final File file = new File(uri.getSchemeSpecificPart());
                return file.canWrite();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // the same inset at both ends keeps the controls centred
    public static int safeSideInset(final int systemLeft, final int systemRight,
                                    final int cutoutLeft, final int cutoutRight) {
        final int left = Math.max(systemLeft, cutoutLeft);
        final int right = Math.max(systemRight, cutoutRight);
        return Math.max(Math.max(left, right), 0);
    }

    public static boolean isSupportedNetworkUri(final Uri uri) {
        if (uri == null)
            return false;
        final String scheme = uri.getScheme();
        if (scheme == null)
            return false;
        return scheme.startsWith("http") || scheme.equals("rtsp");
    }

    public static boolean isLocal(final Uri uri) {
        if (uri == null)
            return false;
        final String scheme = uri.getScheme();
        if (scheme == null)
            return true;
        return "file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme);
    }

    public static String resolvePlayableUriForMpv(final Context context, final Uri uri) {
        if (uri == null) return null;
        final String scheme = uri.getScheme();
        if (scheme == null || "file".equalsIgnoreCase(scheme)) {
            final String path = uri.getPath();
            return path != null ? path : uri.toString();
        }
        if (!"content".equalsIgnoreCase(scheme)) {
            return uri.toString();
        }
        // Strategy 1: Attempt to extract direct local filesystem path
        try {
            final String decoded = Uri.decode(uri.toString());
            final String[] prefixes = new String[]{"/storage/emulated/", "/storage/", "/sdcard/"};
            for (String prefix : prefixes) {
                int index = decoded.indexOf(prefix);
                if (index != -1) {
                    String candidate = decoded.substring(index);
                    int queryIndex = candidate.indexOf('?');
                    if (queryIndex != -1) candidate = candidate.substring(0, queryIndex);
                    int hashIndex = candidate.indexOf('#');
                    if (hashIndex != -1) candidate = candidate.substring(0, hashIndex);
                    java.io.File file = new java.io.File(candidate);
                    if (file.canRead()) {
                        return candidate;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Strategy 2: Query MediaStore DATA column
        try (android.database.Cursor cursor = context.getContentResolver().query(
                uri, new String[]{android.provider.MediaStore.MediaColumns.DATA}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA);
                if (col != -1) {
                    String data = cursor.getString(col);
                    if (data != null && !data.isEmpty() && new java.io.File(data).canRead()) {
                        return data;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: Open file descriptor and pass as fd:// for native libmpv
        try {
            android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                int fd = pfd.detachFd();
                return "fd://" + fd;
            }
        } catch (Exception e) {
            log("resolvePlayableUriForMpv fallback failed: " + e);
        }

        return uri.toString();
    }

    public static boolean isTvBox(Context context) {
        if (Showcase.on(context, Showcase.PREF_TV)) {
            return true;
        }
        final PackageManager pm = context.getPackageManager();

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }

        if (pm.hasSystemFeature(FEATURE_FIRE_TV)) {
            return true;
        }

        // no Files app (DocumentsUI) means a box, though some boxes ship a stub
        if (!hasSAFChooser(pm)) {
            return true;
        }

        // legacy storage no longer works on Android 11 (level 30)
        if (Build.VERSION.SDK_INT < 30) {
            // some boxes still report a touchscreen
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) && !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                return true;
            }

            if (pm.hasSystemFeature("android.hardware.hdmi.cec")) {
                return true;
            }

            if (Build.MANUFACTURER.equalsIgnoreCase("zidoo")) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSAFChooser(final PackageManager pm) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        return intent.resolveActivity(pm) != null;
    }

    public static int normRate(float rate) {
        return (int)(rate * 100f);
    }

    public static Uri convertInputStreamToUTF(Context context, Uri subtitleUri, InputStream inputStream) {
        final String scheme = subtitleUri != null ? subtitleUri.getScheme() : null;
        final boolean isContentUri = scheme != null && "content".equalsIgnoreCase(scheme);
        return convertInputStreamToUTF(context, subtitleUri, inputStream, isContentUri);
    }

    // keepCopy: write a copy even if already UTF-8, and return null on failure
    public static Uri convertInputStreamToUTF(Context context, Uri subtitleUri, InputStream inputStream,
                                              boolean keepCopy) {
        try {
            DecodedInputStreamReader decodedInputStreamReader = Chardet.decode(inputStream, StandardCharsets.UTF_8);
            Charset charset = decodedInputStreamReader.charset();
            if (keepCopy || !StandardCharsets.UTF_8.equals(charset)) {
                // named after the subtitle; a content URI's last segment can be a row id
                final File file = SubtitleFiles.fileFor(context, subtitleUri, null);
                final BufferedReader bufferedReader = new BufferedReader(decodedInputStreamReader);
                final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                char[] buffer = new char[512];
                int num;
                int pass = 0;
                boolean success = true;
                while ((num = bufferedReader.read(buffer)) != -1) {
                    bufferedWriter.write(buffer, 0, num);
                    pass++;
                    if (pass * 512 > 2_000_000) {
                        success = false;
                        break;
                    }
                }
                bufferedWriter.close();
                bufferedReader.close();
                if (success) {
                    subtitleUri = Uri.fromFile(file);
                } else {
                    subtitleUri = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (keepCopy) {
                return null;
            }
        }
        return subtitleUri;
    }

    public static boolean isPiPSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (BuildConfig.FLAVOR_distribution.equals("amazon") && packageManager.hasSystemFeature(FEATURE_FIRE_TV)) {
            return false;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public static Uri getMoviesFolderUri() {
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= 26) {
            final String authority = "com.android.externalstorage.documents";
            final String documentId = "primary:" + Environment.DIRECTORY_MOVIES;
            uri = DocumentsContract.buildDocumentUri(authority, documentId);
        }
        return uri;
    }

    public static boolean isProgressiveContainerUri(final Uri uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        path = path.toLowerCase();
        for (String extension : supportedExtensionsVideo) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getDeviceLanguages() {
        final List<String> locales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 24) {
            final LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
            for (int i = 0; i < localeList.size(); i++) {
                locales.add(localeList.get(i).getISO3Language());
            }
        } else {
            final Locale locale = Resources.getSystem().getConfiguration().locale;
            locales.add(locale.getISO3Language());
        }
        return locales.toArray(new String[0]);
    }

    public static ComponentName getSystemComponent(Context context, Intent intent) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos.size() < 2) {
            return null;
        }
        int systemCount = 0;
        ComponentName componentName = null;
        for (ResolveInfo resolveInfo : resolveInfos) {
            int flags = resolveInfo.activityInfo.applicationInfo.flags;
            boolean system = (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system) {
                systemCount++;
                componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            }
        }
        if (systemCount == 1) {
            return componentName;
        }
        return null;
    }

    public static float normalizeScaleFactor(float scaleFactor, float min) {
        return Math.max(min, Math.min(scaleFactor, 2.0f));
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 720;
    }

    public static <K, V> void orderByValue(LinkedHashMap<K, V> m, final Comparator<? super V> c) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(m.entrySet());
        Collections.sort(entries, (lhs, rhs) -> c.compare(lhs.getValue(), rhs.getValue()));
        m.clear();
        for(Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void scanMediaStorage(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        List<String> storagePaths = new ArrayList<>();
        for (StorageVolume volume : storageVolumes) {
            File directory = volume.getDirectory();
            if (directory != null) {
                storagePaths.add(directory.getAbsolutePath());
            }
        }
        MediaScannerConnection.scanFile(context, storagePaths.toArray(new String[0]), new String[]{"*/*"}, null);
    }

    public static double getFrameRate(Context context, Uri videoUri) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        ArrayList<Long> timestamps = new ArrayList<>();
        double frameRate = Format.NO_VALUE;
        int ignoreSamples = 30;
        try {
            mediaExtractor.setDataSource(context, videoUri, null);
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mimeType = format.getString(MediaFormat.KEY_MIME);
                if (mimeType != null && mimeType.startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                    while (timestamps.size() < 350 + ignoreSamples) {
                        long timestamp = mediaExtractor.getSampleTime();
                        if (timestamp < 0) {
                            break;
                        }
                        timestamps.add(timestamp);
                        mediaExtractor.advance();
                    }
                    break;
                }
            }
            Collections.sort(timestamps);
            long totalFrameDuration = 0;
            for (int i = 1; i < (timestamps.size() - ignoreSamples); i++) {
                totalFrameDuration += (timestamps.get(i) - timestamps.get(i - 1));
            }
            if (timestamps.size() > 1) {
                double averageFrameDuration = (double) totalFrameDuration / (timestamps.size() - ignoreSamples - 1);
                frameRate = 1_000_000.0 / averageFrameDuration;
                if (frameRate > 23.95 && frameRate < 23.988) {
                    frameRate = 24000.0 / 1001.0;
                } else if (frameRate > 23.988 && frameRate < 24.1) {
                    frameRate = 24.0;
                } else if (frameRate > 24.9 && frameRate < 25.1) {
                    frameRate = 25.0;
                } else if (frameRate > 29.95 && frameRate < 29.985) {
                    frameRate = 30000.0 / 1001.0;
                } else if (frameRate > 29.985 && frameRate < 30.1) {
                    frameRate = 30.0;
                } else if (frameRate > 49.9f && frameRate < 50.1) {
                    frameRate = 50.0;
                } else if (frameRate > 59.9f && frameRate < 59.97) {
                    frameRate = 60000.0 / 1001.0;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        } finally {
            mediaExtractor.release();
        }
        return frameRate;
    }

    public static void showFocused(final android.app.AlertDialog dialog, final int button) {
        dialog.show();
        final android.widget.Button target = dialog.getButton(button);
        if (target == null) {
            return;
        }
        target.requestFocus();
        // request again once the window has focus, or it lands on the scroll panel
        final android.view.Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        final android.view.ViewTreeObserver observer = window.getDecorView().getViewTreeObserver();
        observer.addOnWindowFocusChangeListener(new android.view.ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(final boolean hasFocus) {
                if (!hasFocus) {
                    return;
                }
                target.requestFocus();
                final android.view.ViewTreeObserver current = target.getViewTreeObserver();
                if (current.isAlive()) {
                    current.removeOnWindowFocusChangeListener(this);
                }
            }
        });
    }

}

