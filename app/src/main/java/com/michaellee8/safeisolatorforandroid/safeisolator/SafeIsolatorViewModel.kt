package com.michaellee8.safeisolatorforandroid.safeisolator

import android.app.Activity.RESULT_OK
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.michaellee8.safeisolatorforandroid.MainActivity
import com.michaellee8.safeisolatorforandroid.MainActivity.Companion.REQUEST_VPN
import eu.faircode.netguard.ServiceSinkhole
import java.net.InetAddress

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

    var isInternetReachable by mutableStateOf(isInternetAvailable)

    fun onVpnEnabledChange(value: Boolean) {
        if (vpnEnabled != value) {
            vpnEnabled = value
            prefs.edit().putBoolean(SharedPreferencesKeys.ENABLED, value).apply()
            if (value) {
                enableVpn()
            } else {
                disableVpn()
            }
        }
        isInternetReachable = isInternetAvailable
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
                isInternetReachable = isInternetAvailable
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

    val isInternetAvailable: Boolean
        get() {
            try {
                val ipAddr = InetAddress.getByName("google.com")
                Log.d("SafeIsolatorViewModel", "isInternetAvailable: ipAddr: ${ipAddr.hostAddress}")
                return !ipAddr.equals("")
            } catch (e: Exception) {
                return false
            }
        }

    fun setupWorkProfile() {

    }

    fun navigateToAPKDownloadSite() {

    }

    fun pickFileToTransferToWorkProfile() {

    }
}