package com.vibra.wanderai.presentation.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Runtime permission groups requested from the UI. */
object AppPermissions {
    val location = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val camera = arrayOf(Manifest.permission.CAMERA)

    val bluetooth: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}

fun Context.hasAllPermissions(permissions: Array<String>): Boolean =
    permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

fun Context.hasAnyPermission(permissions: Array<String>): Boolean =
    permissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

@Stable
class PermissionState internal constructor(granted: Boolean) {
    var isGranted by mutableStateOf(granted)
        internal set

    internal var launch: () -> Unit = {}

    fun request() = launch()
}

/**
 * Runtime permission holder that re-checks on resume (the user may grant it
 * from system settings). [requireAll] = false accepts any of the permissions,
 * e.g. coarse-only location.
 */
@Composable
fun rememberPermissionState(
    permissions: Array<String>,
    requireAll: Boolean = true,
    onResult: (Boolean) -> Unit = {},
): PermissionState {
    val context = LocalContext.current
    fun check() = if (requireAll) context.hasAllPermissions(permissions) else context.hasAnyPermission(permissions)

    val state = remember(permissions.contentToString()) { PermissionState(check()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = if (requireAll) result.values.all { it } else result.values.any { it }
        state.isGranted = granted || check()
        onResult(state.isGranted)
    }
    state.launch = { launcher.launch(permissions) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.isGranted = check() }
    return state
}
