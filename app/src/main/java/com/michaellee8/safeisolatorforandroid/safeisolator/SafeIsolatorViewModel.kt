package com.michaellee8.safeisolatorforandroid.safeisolator

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.preference.PreferenceManager

class SafeIsolatorViewModel(val app: Application) : AndroidViewModel(app) {

    val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())

    var vpnEnabled by mutableStateOf(false)

    fun onVpnEnabledChange(value: Boolean) {
        if (vpnEnabled != value) {
            vpnEnabled = value
            if (value) {
                enableVpn()
            } else {
                disableVpn()
            }
        }
    }

    fun enableVpn() {

    }

    fun disableVpn() {

    }

}