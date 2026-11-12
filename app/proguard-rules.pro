# private fields read by reflection; R8 would rename them in release builds
-keepclassmembers class androidx.media3.ui.DefaultTimeBar {
    private android.graphics.Rect seekBounds;
    private android.graphics.Rect progressBar;
    private android.graphics.Rect bufferedBar;
    private android.graphics.Rect scrubberBar;
}
-keepclassmembers class androidx.media3.ui.PlayerControlView {
    private androidx.media3.ui.TrackNameProvider trackNameProvider;
    private android.graphics.drawable.Drawable playButtonDrawable;
    private android.graphics.drawable.Drawable pauseButtonDrawable;
}

# libmpv native JNI callbacks
-keep class is.xyz.mpv.** { *; }

# Keep dynamic reflection & annotation handlers for SMBJ / MBassy / JSch / Commons-Net / NanoHTTPD
-keep class com.hierynomus.** { *; }
-keep class net.engio.mbassy.** { *; }
-keep class com.jcraft.jsch.** { *; }
-keep class fi.iki.elonen.** { *; }
-keep class org.nanohttpd.** { *; }

# Optional Java EE / Kerberos GSS references unused on Android (from smbj/mbassy)
-dontwarn javax.el.BeanELResolver
-dontwarn javax.el.ELContext
-dontwarn javax.el.ELResolver
-dontwarn javax.el.ExpressionFactory
-dontwarn javax.el.FunctionMapper
-dontwarn javax.el.ValueExpression
-dontwarn javax.el.VariableMapper
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn org.ietf.jgss.MessageProp

# Optional desktop-only JSch Pageant / Log4j2 / JUnixSocket references unused on Android
-dontwarn com.sun.jna.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.newsclub.net.unix.**
