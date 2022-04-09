package com.michaellee8.safeisolatorforandroid.safeisolator

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun SafeIsolatorMainScreen(
    enableVpn: () -> Unit,
    disableVpn: () -> Unit,
    setupWorkProfile: () -> Unit,
    navigateToAPKDownloadSite: () -> Unit,
    pickFileToTransferToWorkProfile: () -> Unit,
    isInstalledOnWorkProfile: Boolean,
    vpnEnabled: Boolean,
) {
    Column {
        if (isInstalledOnWorkProfile) {
            Text(text = "Running in Work Profile.")
            Text(text = "1. Setup VPN.")
            if (vpnEnabled) {
                Text(text = "VPN is already running, you are protected!")
            }
        } else {
            Text(text = "Not running in Work Profile.")
            Text(text = "1. Setup work profile.")
            Text(text = "2. Download the APK of app to be isolated.")
            Text(text = "3. Pick the APK file to transfer to the work profile.")
            Text(text = "4. Install the app in the work profile.")
        }
        if (isInstalledOnWorkProfile) {
            if (vpnEnabled) {
                Button(onClick = { disableVpn() }) {
                    Text("Disable VPN")
                }
            } else {
                Button(onClick = { enableVpn() }) {
                    Text("Enable VPN")
                }
            }
        } else {
            Button(onClick = { setupWorkProfile() }) {
                Text("Setup work profile")
            }
            Button(onClick = { pickFileToTransferToWorkProfile() }) {
                Text("Pick file to be transferred")
            }
        }


    }
}