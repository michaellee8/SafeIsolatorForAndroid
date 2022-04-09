package com.michaellee8.safeisolatorforandroid.safeisolator

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.michaellee8.safeisolatorforandroid.MainActivity

class SafeIsolatorViewModelFactory(
    val app: Application, val activity: MainActivity,
    val onActivityResult: (Int, Int, Intent?) -> Unit
) : ViewModelProvider.AndroidViewModelFactory(app) {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        SafeIsolatorViewModel(app, activity, onActivityResult) as T
}
