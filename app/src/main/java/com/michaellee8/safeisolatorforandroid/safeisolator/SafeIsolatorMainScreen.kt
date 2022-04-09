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
        if (vpnEnabled) {
            Button(onClick = { disableVpn() }) {
                Text("Disable VPN")
            }
        } else {
            Button(onClick = { enableVpn() }) {
                Text("Enable VPN")
            }
        }


    }
}