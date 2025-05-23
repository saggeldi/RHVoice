package com.github.olga_yakovleva.rhvoice.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.olga_yakovleva.rhvoice.compose.camera.CameraScreen
import com.github.olga_yakovleva.rhvoice.compose.no_permission.NoPermissionScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(listener: MainFragmentListener) {

    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    MainContent(
        hasPermission = cameraPermissionState.status.isGranted,
        onSettingsClick = {
            listener.onSettingsClick()
        },
        onRequestPermission = cameraPermissionState::launchPermissionRequest
    )
}

@Composable
private fun MainContent(
    hasPermission: Boolean,
    onSettingsClick: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                onSettingsClick()
            }
        ) {
            Text("Open settings")
        }
        if (hasPermission) {
            CameraScreen()
        } else {
            NoPermissionScreen(onRequestPermission)
        }
    }
}

@Preview
@Composable
private fun Preview_MainContent() {
    MainContent(
        hasPermission = true,
        onSettingsClick = {},
        onRequestPermission = {}
    )
}