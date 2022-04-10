package com.michaellee8.safeisolatorforandroid

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
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
import net.typeblog.shelter.receivers.ShelterDeviceAdminReceiver
import net.typeblog.shelter.util.LocalStorageManager
import net.typeblog.shelter.util.Utility

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

    private var mStorage: LocalStorageManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStorageManager.initialize(applicationContext)
        mStorage = LocalStorageManager.getInstance()
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

    val mProvisionProfile =
        registerForActivityResult(ProfileProvisionContract()
        ) { result: Boolean? ->
            this.setupProfileCb(result)
        }


    class ProfileProvisionContract() :
        ActivityResultContract<Void?, Boolean?>() {

        override fun createIntent(context: Context, input: Void?): Intent {
            val admin = ComponentName(context.applicationContext,
                ShelterDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                admin)
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == RESULT_OK
        }
    }

    private fun setupProfileCb(result: Boolean?) {
        if (result != null && result) {
            if (Utility.isWorkProfileAvailable(this)) {
                Log.d("SafeIsolatorMainActivity", "Work profile setup already done")
                return
            }

            mStorage?.setBoolean(LocalStorageManager.PREF_IS_SETTING_UP, true)
            Log.d("SafeIsolatorMainActivity", "Work profile setup success")
        } else {
            Log.d("SafeIsolatorMainActivity", "Work profile setup failed")
        }
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