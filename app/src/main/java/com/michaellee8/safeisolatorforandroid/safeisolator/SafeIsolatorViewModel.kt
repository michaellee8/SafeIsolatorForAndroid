package com.michaellee8.safeisolatorforandroid.safeisolator

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.util.Log
import android.view.LayoutInflater
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.preference.PreferenceManager
import com.michaellee8.safeisolatorforandroid.MainActivity
import com.michaellee8.safeisolatorforandroid.MainActivity.Companion.REQUEST_VPN
import eu.faircode.netguard.ServiceSinkhole

object SharedPreferencesKeys {
    const val ENABLED = "enabled"
    const val LOCKDOWN = "lockdown"
}

class SafeIsolatorViewModel(
    val app: Application,
    val activity: MainActivity,
    val onActivityResult: (Int, Int, Intent?) -> Unit
) : AndroidViewModel(app) {

    val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())

    var vpnEnabled by mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLED, false))

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
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "enabled") {
                vpnEnabled = sharedPreferences.getBoolean(key, false)
            }
        }
        prefs.edit().putBoolean(SharedPreferencesKeys.LOCKDOWN, true).apply()
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

    //
    fun disableVpn() {
        ServiceSinkhole.stop("switch off", getApplication(), false)
    }

}