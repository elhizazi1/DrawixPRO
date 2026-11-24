package xyz.siwane.drawix.pro.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.components.*

@Composable fun PermissionsScreen(vm: DrawViewModel, onPermission: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHero(text(R.string.nav_permissions), text(R.string.permissions_manage_title), R.drawable.ic_shield_tick) }
        item { InfoCard(text(R.string.permissions_privacy_title), text(R.string.permissions_privacy_desc), R.drawable.ic_shield_tick) }
        item { SectionTitle(text(R.string.permissions_manage_title)) }
        val permissions = listOf(
            PermissionRow("overlay", R.string.permissions_overlay_title, R.string.permissions_overlay_desc, R.drawable.ic_maximize_4, vm.hasOverlay()),
            PermissionRow("accessibility", R.string.permissions_accessibility_title, R.string.permissions_accessibility_desc, R.drawable.ic_security_safe, vm.hasAccessibility()),
            PermissionRow("storage", R.string.permissions_storage_title, R.string.ui_storage_desc, R.drawable.ic_gallery, vm.hasStorage()),
            PermissionRow("notifications", R.string.permissions_notifications_title, R.string.permissions_notifications_desc, R.drawable.ic_info_circle, vm.hasNotifications()),
            PermissionRow("battery", R.string.permissions_battery_title, R.string.ui_battery_desc, R.drawable.ic_battery_full, vm.batteryExempt())
        )
        permissions.forEachIndexed { index, permission -> item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = CardCorners.grouped(index, permissions.size)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) { DrawIcon(permission.icon, modifier = Modifier.padding(8.dp).size(22.dp)) }
                        Text(text(permission.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (permission.granted) DrawIcon(R.drawable.ic_tick_circle, text(R.string.btn_permission_granted))
                    }
                    Text(text(permission.description), style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(onClick = { onPermission(permission.key) }) {
                        Text(text(if (permission.granted) R.string.ui_manage else R.string.btn_grant_settings))
                    }
                }
            }
        } }
        item { InfoCard(text(R.string.permissions_device_note_title), text(R.string.ui_device_note), R.drawable.ic_info_circle) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
private data class PermissionRow(val key: String, val title: Int, val description: Int, val icon: Int, val granted: Boolean)
