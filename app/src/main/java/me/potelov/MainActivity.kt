package me.potelov

import android.Manifest
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.potelov.databinding.ActivityMainBinding
import me.potelov.util.SEFPhones
import me.potelov.util.extension.phoneCall

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val telephonyManager by lazy { getSystemService(TELEPHONY_SERVICE) as TelephonyManager }

    private val callState = MutableSharedFlow<CallDto>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val scope = MainScope()

    private var mediaPlayer: MediaPlayer? = null

    private var timer: CountDownTimer? = null

    private var count = 0

    private val callStateListener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            count = count.plus(1)
            Log.d("vova", "count = $count")
            scope.launch {
                callState.emit(CallDto(count, state))
            }
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d("vova", "CALL_STATE_IDLE")
                    timer?.cancel()
                    call()
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("vova", "CALL_STATE_OFFHOOK")
                }

                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d("vova", "CALL_STATE_RINGING")
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            val allPermissionsGranted = permissions.all { it.value }
            if (!allPermissionsGranted) {
                Log.d("vova", "Permissions not granted")
            } else {
                Log.d("vova", "Permissions granted")
            }
        }

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupChips()
        with(binding) {
            btnStart.setOnClickListener {
                setupTelephonyManager()
            }
            tilPhone.setEndIconOnClickListener {
                performViewHapticFeedback()
                etPhone.text?.clear()
                for (i in 0 until chipGroup.childCount) {
                    val child = chipGroup.getChildAt(i)
                    if (child is Chip) {
                        child.isChecked = false
                    }
                }
            }
            tvStopMusic.setOnClickListener {
                mediaPlayer?.release()
            }
        }
        requestPermissions()
        mediaPlayer = MediaPlayer.create(this, R.raw.t).apply {
            setOnCompletionListener {
                binding.tvStopMusic.isVisible = false
            }
        }
        callState
            .filter { it.state == TelephonyManager.CALL_STATE_OFFHOOK }
            .debounce(3000)
            .onEach {
                Log.d("vova", "dto = $it")
                startTimer()
            }
            .launchIn(scope)
    }

    override fun onDestroy() {
        telephonyManager.unregisterTelephonyCallback(callStateListener)
        timer?.cancel()
        mediaPlayer?.release()
        super.onDestroy()
    }

    private fun setupChips() {
        SEFPhones.forEach { text ->
            val chip = Chip(this@MainActivity)
            chip.text = text
            chip.isCheckable = true
            chip.isClickable = true
            chip.setOnClickListener {
                performViewHapticFeedback()
                binding.etPhone.text?.clear()
                if (binding.chipGroup.checkedChipIds.isNotEmpty()) {
                    binding.etPhone.setText(text)
                    binding.etPhone.setSelection(text.length)
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
            )
        )
    }

    private fun performViewHapticFeedback() {
        binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun setupTelephonyManager() {
        telephonyManager.registerTelephonyCallback(mainExecutor, callStateListener)
    }

    private fun call() {
        val text = binding.etPhone.text?.toString()
        if (!text.isNullOrEmpty()) {
            phoneCall(text)
        }
    }

    private fun startTimer() {
        timer?.cancel()
        Log.d("vova", "start")
        timer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(4), 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                Log.d("vova", "onFinish")
                mediaPlayer?.start()
                binding.tvStopMusic.isVisible = true
            }
        }

        timer?.start()
    }

}

data class CallDto(val count: Int, val state: Int)
