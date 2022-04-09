package com.michaellee8.safeisolatorforandroid

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.system.exitProcess

class SafeIsolatorAppliction : Application() {
    companion object {
        val TAG = "NetGuard.App"
    }

    private var mPrevHandler: Thread.UncaughtExceptionHandler? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        mPrevHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            exitProcess(1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val foreground = NotificationChannel(
            "foreground",
            getString(R.string.channel_foreground),
            NotificationManager.IMPORTANCE_MIN
        )
        foreground.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        nm.createNotificationChannel(foreground)

        val notify = NotificationChannel(
            "notify",
            getString(R.string.channel_notify),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notify.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        notify.setBypassDnd(true)
        nm.createNotificationChannel(notify)

        val access = NotificationChannel(
            "access",
            getString(R.string.channel_access),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        access.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        access.setBypassDnd(true)
        nm.createNotificationChannel(access)
    }
}