# Battery usage

Drawix Pro keeps a foreground service active while floating drawing tools are enabled. A persistent notification makes that service visible. Disable the floating service when it is no longer needed.

Drawing updates respond to input and tool actions. Laser fades and overlay transitions are finite animations. Screenshot commands run on demand. The capture engines do not continuously take screenshots while idle.

The application schedules bounded notification work through Android WorkManager. WorkManager declares the WAKE_LOCK permission and manages wake locks while its tasks execute; this differs from an application holding a permanent wake lock. No application-owned, long-held wake lock is used by the drawing service.

Visible interface animations, active drawing, repeated capture and external privilege services can all affect consumption. Root, Shizuku and LSPosed have their own processes and system interactions; Android may attribute their energy use separately.

## Compare usage

Use Android battery settings to compare similar periods with the floating service enabled and disabled, including a screen-off idle period. Record elapsed time, screen-on time, drawing/capture activity and battery change. A percentage since the last full charge, without elapsed time and workload, cannot establish hourly drain.

Battery consumption varies with device, display refresh rate, ROM and power management. Code review and successful builds are not substitutes for physical power measurements. Battery optimization exemptions are optional and should be used only when background tools are being stopped unexpectedly.
