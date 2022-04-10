package com.michaellee8.safeisolatorforandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.michaellee8.safeisolatorforandroid.safeisolator.SafeIsolatorMainScreen
import com.michaellee8.safeisolatorforandroid.safeisolator.SafeIsolatorViewModel
import com.michaellee8.safeisolatorforandroid.safeisolator.SafeIsolatorViewModelFactory
import com.michaellee8.safeisolatorforandroid.ui.theme.SafeIsolatorForAndroidTheme

class MainActivity : ComponentActivity() {

    val safeIsolatorViewModel by viewModels<SafeIsolatorViewModel> {
        SafeIsolatorViewModelFactory(
            application,
            this,
            onActivityResult = { requestCode: Int, resultCode: Int, data: Intent? ->
                this.onActivityResult(
                    requestCode = requestCode,
                    resultCode = resultCode,
                    data = data
                )
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeIsolatorForAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    SafeIsolatorMainActivityScreen(safeIsolatorViewModel = safeIsolatorViewModel)
                }
            }
        }
    }

    companion object {
        const val REQUEST_VPN = 1
        const val REQUEST_INVITE = 2
        const val REQUEST_LOGCAT = 3
        const val REQUEST_ROAMING = 4
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VPN) {
            Toast.makeText(applicationContext, "Starting VPN", Toast.LENGTH_SHORT).show()
            safeIsolatorViewModel.startVpn()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun SafeIsolatorMainActivityScreen(safeIsolatorViewModel: SafeIsolatorViewModel) {
    SafeIsolatorMainScreen(
        enableVpn = safeIsolatorViewModel::enableVpn,
        disableVpn = safeIsolatorViewModel::disableVpn,
        setupWorkProfile = safeIsolatorViewModel::setupWorkProfile,
        navigateToAPKDownloadSite = safeIsolatorViewModel::navigateToAPKDownloadSite,
        pickFileToTransferToWorkProfile = safeIsolatorViewModel::pickFileToTransferToWorkProfile,
        vpnEnabled = safeIsolatorViewModel.vpnEnabled,
        isInstalledOnWorkProfile = safeIsolatorViewModel.isInstalledOnWorkProfile,
        isInternetReachable = safeIsolatorViewModel.isInternetReachable
    )
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SafeIsolatorForAndroidTheme {
        SafeIsolatorMainScreen(
            enableVpn = {},
            disableVpn = {},
            setupWorkProfile = {},
            navigateToAPKDownloadSite = {},
            pickFileToTransferToWorkProfile = {},
            vpnEnabled = false,
            isInstalledOnWorkProfile = true,
            isInternetReachable = true
        )
    }
}