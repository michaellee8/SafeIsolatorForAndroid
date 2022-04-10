package com.michaellee8.safeisolatorforandroid

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import net.typeblog.shelter.services.FileShuttleService
import net.typeblog.shelter.services.ShelterService
import net.typeblog.shelter.util.LocalStorageManager
import net.typeblog.shelter.util.SettingsManager
import kotlin.system.exitProcess

class SafeIsolatorAppliction : Application() {

    private var mShelterServiceConnection: ServiceConnection? = null
    private var mFileShuttleServiceConnection: ServiceConnection? = null

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

        LocalStorageManager.initialize(this)
        SettingsManager.initialize(this)
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

    fun bindShelterService(conn: ServiceConnection, foreground: Boolean) {
        unbindShelterService()
        val intent = Intent(applicationContext,
            ShelterService::class.java)
        intent.putExtra("foreground", foreground)
        bindService(intent, conn, BIND_AUTO_CREATE)
        mShelterServiceConnection = conn
    }

    fun bindFileShuttleService(conn: ServiceConnection) {
        unbindFileShuttleService()
        val intent = Intent(applicationContext,
            FileShuttleService::class.java)
        bindService(intent, conn, BIND_AUTO_CREATE)
        mFileShuttleServiceConnection = conn
    }

    fun unbindShelterService() {
        if (mShelterServiceConnection != null) {
            try {
                unbindService(mShelterServiceConnection!!)
            } catch (e: Exception) {
                // This method call might fail if the service is already unbound
                // just ignore anything that might happen.
                // We will be stopping already if this would ever happen.
            }
        }
        mShelterServiceConnection = null
    }

    fun unbindFileShuttleService() {
        if (mFileShuttleServiceConnection != null) {
            try {
                unbindService(mFileShuttleServiceConnection!!)
            } catch (e: Exception) {
                // ...
            }
        }
        mFileShuttleServiceConnection = null
    }
}