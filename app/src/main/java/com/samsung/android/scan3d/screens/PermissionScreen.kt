package com.samsung.android.scan3d.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.samsung.android.scan3d.R

@Composable
fun PermissionScreen(
    onNavigateToCamera: () -> Unit,
    onServiceStart: () -> Unit
) {
    val context = LocalContext.current

    val permissionsToRequest = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                onServiceStart()
                onNavigateToCamera()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.perm_toast_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    PermissionScreenUI(
        onAuthorizeClicked = {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    )
}
