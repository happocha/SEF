package me.potelov.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import me.potelov.App

val Activity.app get() = application as App

fun Activity.phoneCall(tel: String) {
    val callIntent = Intent(Intent.ACTION_CALL)
    callIntent.data = Uri.parse("tel:$tel")
    startActivity(callIntent)
}

@Suppress("DEPRECATION")
val Context.vibratorService: Vibrator
    get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

val Context.vibratorManagerService: VibratorManager
    get() = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager


@RequiresPermission(android.Manifest.permission.VIBRATE)
fun Activity.vibratePhone(milli: Long = 50) {
    val service = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.vibratorManagerService?.defaultVibrator
    } else {
        this.vibratorService
    }
    if (service != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.vibrate(VibrationEffect.createOneShot(milli, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            service.vibrate(milli)
        }
    }
}
