package com.michaellee8.safeisolatorforandroid.safeisolator

import android.app.Activity.RESULT_OK
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.michaellee8.safeisolatorforandroid.MainActivity
import com.michaellee8.safeisolatorforandroid.MainActivity.Companion.REQUEST_VPN
import eu.faircode.netguard.ServiceSinkhole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.typeblog.shelter.util.ApplicationInfoWrapper
import net.typeblog.shelter.util.AuthenticationUtility
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

object SharedPreferencesKeys {
    const val ENABLED = "enabled"
    const val LOCKDOWN = "lockdown"
}

class SafeIsolatorViewModel(
    val app: Application,
    val activity: MainActivity,
    val onActivityResult: (Int, Int, Intent?) -> Unit,
) : AndroidViewModel(app) {

    val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())

    var vpnEnabled by mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLED, false))

    var isInternetReachable by mutableStateOf(true)

    fun onVpnEnabledChange(value: Boolean) {
        Log.d("SafeIsolatorViewModel", "$vpnEnabled $value")
        if (vpnEnabled != value) {
            vpnEnabled = value
            prefs.edit().putBoolean(SharedPreferencesKeys.ENABLED, value).apply()
            if (value) {
                enableVpn()
            } else {
                disableVpn()
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "enabled") {
                vpnEnabled = sharedPreferences.getBoolean(key, false)
            }
        }
        prefs.edit().putBoolean(SharedPreferencesKeys.LOCKDOWN, true).apply()

        val timerHandler = Handler()

        val refreshIsInternetReachableRunnable = object : Runnable {
            override fun run() {
                performAndScheduleInternetCheck()
                timerHandler.postDelayed(this, 3000)
            }
        }

        timerHandler.post(refreshIsInternetReachableRunnable)
    }

    fun enableVpn() {
        val prepare = VpnService.prepare(getApplication())
        if (prepare == null) {
            onActivityResult(REQUEST_VPN, RESULT_OK, null)
        } else {
            activity.startActivityForResult(prepare, MainActivity.REQUEST_VPN)
        }
    }

    // Would be called from MainActivity::onActivityResult
    fun startVpn() {
        ServiceSinkhole.start("enabled", activity)
        Log.i("SafeIsolator", "Called ServiceSinkhole::start")
    }

    fun disableVpn() {
        ServiceSinkhole.stop("switch off", getApplication(), false)
    }

    val isInstalledOnWorkProfile: Boolean
        get() = app.let { it ->
            val devicePolicyManager =
                it.getSystemService(AppCompatActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val activeAdmins = devicePolicyManager.activeAdmins

            activeAdmins?.any { devicePolicyManager.isProfileOwnerApp(it.packageName) } ?: false
        }

    suspend fun makeInternetCheckRequest(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val url = URL("https://google.com")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Cache-Control", "no-cache")
            conn.defaultUseCaches = false
            conn.useCaches = false
            conn.runCatching {
                requestMethod = "POST"
                doInput = true
                val byteArray = inputStream.readBytes()
                if (byteArray.isEmpty()) {
                    throw Exception("Empty byteArray in inputStream")
                }
                Log.d("makeInternetCheckRequest", String(bytes = byteArray, StandardCharsets.UTF_8))
                return@runCatching
            }
        }
    }

    fun performAndScheduleInternetCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                makeInternetCheckRequest()
//                Log.d("SafeIsolatorViewModel", "isInternetAvailable: true")
                isInternetReachable = true
            } catch (e: Exception) {
//                Log.d("SafeIsolatorViewModel", "isInternetAvailable: exception: $e")
                isInternetReachable = false
            }
        }
    }

    fun setupWorkProfile() {
        val policyManager = app.getSystemService(DevicePolicyManager::class.java)
        if (!policyManager.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            Log.d("Shelter", "Provision failed.")
        }
        AuthenticationUtility.reset()
        activity.mProvisionProfile.launch(null)
    }

    fun getInstalledApps(): List<ApplicationInfoWrapper> {
        val pmFlags =
            PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        val mPackageManager = app.packageManager
        val list = mPackageManager.getInstalledApplications(pmFlags)
            .stream()
            .filter { it: ApplicationInfo -> it.packageName != app.packageName }
            .map { info: ApplicationInfo? ->
                ApplicationInfoWrapper(info)
            }
            .sorted { x: ApplicationInfoWrapper, y: ApplicationInfoWrapper ->
                // Sort hidden apps at the last
                if (x.isHidden && !y.isHidden) {
                    return@sorted 1
                } else if (!x.isHidden && y.isHidden) {
                    return@sorted -1
                } else {
                    return@sorted x.label.compareTo(y.label)
                }
            }
            .collect(Collectors.toList())
        return list
    }

    fun installApk() {
        activity.startInstallApk()
    }

    fun navigateToAPKDownloadSite() {

    }

    fun pickFileToTransferToWorkProfile() {
        installApk()
    }

    fun transferChromeToWorkProfile() {
        activity.installChromeToWorkProfile()
    }
}