package com.cmstudio.player;

import android.app.Application;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import coil.ComponentRegistry;
import coil.ImageLoader;
import coil.ImageLoaderFactory;
import coil.decode.VideoFrameDecoder;
import coil.disk.DiskCache;

import java.io.File;

// background crashes are logged; the main thread keeps the default handler
public final class App extends Application implements ImageLoaderFactory {

    @Override
    public void onCreate() {
        super.onCreate();
        AppLanguage.start(this);
        Showcase.start(this);
        AppTheme.start(this);

        final Thread.UncaughtExceptionHandler existing =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            if (thread == getMainLooper().getThread() || existing == null) {
                if (existing != null) {
                    existing.uncaughtException(thread, error);
                }
                return;
            }
            Utils.log("Background thread " + thread.getName() + " failed: " + error);
            if (BuildConfig.DEBUG) {
                error.printStackTrace();
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration config) {
        super.onConfigurationChanged(config);
        AppLanguage.keep(this);
    }

    @NonNull
    @Override
    public ImageLoader newImageLoader() {
        int cacheMb = 128;
        try {
            cacheMb = getSharedPreferences("mpvx_player_prefs", MODE_PRIVATE)
                    .getInt("pref_thumbnail_cache_mb", 128);
            if (cacheMb < 32 || cacheMb > 512) cacheMb = 128;
        } catch (Throwable ignored) {
        }
        long maxDiskBytes = (long) cacheMb * 1024L * 1024L;
        return new ImageLoader.Builder(this)
                .components(new ComponentRegistry.Builder()
                        .add(new VideoFrameDecoder.Factory())
                        .build())
                .diskCache(() -> new DiskCache.Builder()
                        .directory(new File(getCacheDir(), "image_cache"))
                        .maxSizeBytes(maxDiskBytes)
                        .build())
                .crossfade(true)
                .build();
    }
}

