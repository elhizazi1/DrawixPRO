-keepattributes SourceFile,LineNumberTable
# Loaded by the Xposed runtime rather than by app code.
-keep class xyz.siwane.drawix.pro.LSPosedModule { *; }
-keep class xyz.siwane.drawix.pro.EngineManager { *; }
-keep class xyz.siwane.drawix.pro.engine.ShizukuCaptureService { *; }
-dontwarn de.robv.android.xposed.**
# Community translation keys are resolved by resource entry name.
-keep class xyz.siwane.drawix.pro.R$string { *; }
# Keep injected framework code isolated from ordinary app classes during R8 merging.
-keep class xyz.siwane.drawix.pro.xposed.** { *; }

# Injected receiver must keep its standalone framework-only boundary.
-keep class xyz.siwane.drawix.pro.capture.LSPosedReceiver** { *; }
