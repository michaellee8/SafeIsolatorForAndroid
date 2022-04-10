package com.michaellee8.safeisolatorforandroid

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
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
import net.typeblog.shelter.services.IAppInstallCallback
import net.typeblog.shelter.services.IShelterService
import net.typeblog.shelter.services.IStartActivityProxy
import net.typeblog.shelter.ui.DummyActivity
import net.typeblog.shelter.util.LocalStorageManager
import net.typeblog.shelter.util.SettingsManager
import net.typeblog.shelter.util.UriForwardProxy
import net.typeblog.shelter.util.Utility
import net.typeblog.shelter.util.Utility.ActivityResultContractInputWrapper

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

    // Two services running in main / work profile
    private var mServiceMain: IShelterService? = null
    private var mServiceWork: IShelterService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStorageManager.initialize(applicationContext)
        mStorage = LocalStorageManager.getInstance()
        SettingsManager.getInstance().applyAll()
        bindServices()
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

    private val mSelectApk = registerForActivityResult(
        ActivityResultContractInputWrapper(
            ActivityResultContracts.OpenDocument(),
            arrayOf("application/vnd.android.package-archive")),
        ActivityResultCallback { uri: Uri? ->
            this.onApkSelectedForInstall(uri)
        })


    class ProfileProvisionContract :
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

    private val mTryStartWorkService =
        registerForActivityResult<Intent, ActivityResult>(ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            this.tryStartWorkServiceCb(result)
        }
    private val mBindWorkService =
        registerForActivityResult<Intent, ActivityResult>(ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            this.bindWorkServiceCb(result)
        }

    private fun bindServices() {
        // Bind to the service provided by this app in main user
        // The service in main profile doesn't need to be foreground
        // because this activity will hold a ServiceConnection to the service
        (application as SafeIsolatorAppliction).bindShelterService(object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mServiceMain = IShelterService.Stub.asInterface(service)
                tryStartWorkService()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                // dummy
            }
        }, false)
    }

    private fun tryStartWorkService() {
        // Send a dummy intent to the work profile first
        // to determine if work mode is enabled and we CAN start something in that profile.
        // If work mode is disabled when starting this app, we will receive RESULT_CANCELED
        // in the activity result, and we can then prompt the user to enable it
        val intent = Intent(DummyActivity.TRY_START_SERVICE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            Utility.transferIntentToProfile(this, intent)
        } catch (e: IllegalStateException) {
            // This exception implies a missing work profile, NOT a disabled work profile
            // which means that the work profile does not even exist
            // in the first place.
            mStorage!!.setBoolean(LocalStorageManager.PREF_HAS_SETUP, false)
            Toast.makeText(this,
                "Work profile not found, please initialize first and then restart the app.",
                Toast.LENGTH_LONG)
                .show()
            return
        }
        mTryStartWorkService.launch(intent)
    }

    private fun tryStartWorkServiceCb(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            // RESULT_OK is from DummyActivity. The work profile is enabled!
            bindWorkService()
        } else {
            // In this case, the user has been presented with a prompt
            // to enable work mode, but we have no means to distinguish
            // "ok" and "cancel", so the only way is to tell the user
            // to start again.
            Toast.makeText(this,
                getString(R.string.work_mode_disabled), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun bindWorkService() {
        // Bind to the ShelterService in work profile
        val intent = Intent(DummyActivity.START_SERVICE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        Utility.transferIntentToProfile(this, intent)
        mBindWorkService.launch(intent)
    }

    private fun bindWorkServiceCb(result: ActivityResult) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            val extra = result.data!!.getBundleExtra("extra")
            val binder = extra!!.getBinder("service")
            mServiceWork = IShelterService.Stub.asInterface(binder)
            registerStartActivityProxies()
        }
    }

    private fun registerStartActivityProxies() {
        try {
            mServiceMain!!.setStartActivityProxy(object : IStartActivityProxy.Stub() {
                @Throws(RemoteException::class)
                override fun startActivity(intent: Intent) {
                    this@MainActivity.startActivity(intent)
                }
            })
            mServiceWork!!.setStartActivityProxy(object : IStartActivityProxy.Stub() {
                @Throws(RemoteException::class)
                override fun startActivity(intent: Intent) {
                    // Using the full intent may cause the package manager to
                    // fail to find the DummyActivity inside profile.
                    // Instead we try to use an empty intent with only the action
                    // and then extract the correct component name
                    val dummyIntent = Intent(intent.action)
                    Utility.transferIntentToProfileUnsigned(this@MainActivity, dummyIntent)
                    intent.component = dummyIntent.component
                    this@MainActivity.startActivity(intent)
                }
            })
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    fun onApkSelectedForInstall(uri: Uri?) {
        if (uri == null) {
            return
        }
        val proxy = UriForwardProxy(applicationContext, uri)

        try {
            mServiceWork!!.installApk(proxy, object : IAppInstallCallback.Stub() {
                override fun callback(result: Int) {
                    runOnUiThread {
                        // The other side will have closed the Fd for us
                        if (result == RESULT_OK) Toast.makeText(this@MainActivity,
                            R.string.install_app_to_profile_success, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            })
        } catch (e: RemoteException) {
            // Well, I don't know what to do then
        }
    }

    fun startInstallApk() {
        mSelectApk.launch(null)
    }

    fun installChromeToWorkProfile() {
        val chromeApp = safeIsolatorViewModel.getInstalledApps().findLast {
            it.info.packageName == "com.android.chrome"
        }
        if (chromeApp == null) {
            Log.e("SafeIsolatorMainActivity", "Chrome App is not round")
            return
        }
        this.mServiceWork?.installApp(chromeApp, object : IAppInstallCallback.Stub() {
            override fun callback(result: Int) {
                Log.i("SafeIsolatorMainActivity", "Install Chrome result: $result")
            }

        })
    }
}

@Composable
fun SafeIsolatorMainActivityScreen(safeIsolatorViewModel: SafeIsolatorViewModel) {
    SafeIsolatorMainScreen(
        onVpnEnabledChange = safeIsolatorViewModel::onVpnEnabledChange,
        setupWorkProfile = safeIsolatorViewModel::setupWorkProfile,
        navigateToAPKDownloadSite = safeIsolatorViewModel::navigateToAPKDownloadSite,
        pickFileToTransferToWorkProfile = safeIsolatorViewModel::pickFileToTransferToWorkProfile,
        vpnEnabled = safeIsolatorViewModel.vpnEnabled,
        isInstalledOnWorkProfile = safeIsolatorViewModel.isInstalledOnWorkProfile,
        isInternetReachable = safeIsolatorViewModel.isInternetReachable,
        transferChromeToWorkProfile = safeIsolatorViewModel::transferChromeToWorkProfile,
    )
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SafeIsolatorForAndroidTheme {
        SafeIsolatorMainScreen(
            onVpnEnabledChange = {},
            setupWorkProfile = {},
            navigateToAPKDownloadSite = {},
            pickFileToTransferToWorkProfile = {},
            vpnEnabled = false,
            isInstalledOnWorkProfile = true,
            isInternetReachable = true,
            transferChromeToWorkProfile = {},
        )
    }
}