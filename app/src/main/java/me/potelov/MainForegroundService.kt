package me.potelov

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.properties.Delegates
import me.potelov.util.OPEN_SEF_MENU_ACTION

class MainForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "MyForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private var tel: String = ""

    private var telephonyManagerState: Int by Delegates.observable(-1) { _, oldValue, newValue ->
        Log.d("vova", "old value = $oldValue")
        Log.d("vova", "new value = $newValue")
        if (newValue != -1) {
            when (newValue) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    callPhone()
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {}
                TelephonyManager.CALL_STATE_RINGING -> {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val callStateListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                telephonyManagerState = state
            }
        }
    } else {
        null
    }

    @Suppress("DEPRECATION")
    private val phoneStateListener: PhoneStateListener? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                telephonyManagerState = state
            }
        }
    } else {
        null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tel = intent?.getStringExtra("phone").orEmpty()
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(true)
            enableVibration(true)
            setSound(null, null)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = OPEN_SEF_MENU_ACTION
        }
        val openPendingIntent: PendingIntent = PendingIntent.getActivity(application, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SEF")
            .setContentText("Are you wanna stop?")
            .setSmallIcon(R.drawable.ic_stop)
            .addAction(0, "STOP", openPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        setupTelephonyManager()
        callPhone()
        return START_NOT_STICKY
    }

    private fun setupTelephonyManager() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(mainExecutor, callStateListener!!)
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun callPhone() {
        Log.d("vova", "call = $tel")
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$tel")
        startActivity(callIntent)
    }
}
