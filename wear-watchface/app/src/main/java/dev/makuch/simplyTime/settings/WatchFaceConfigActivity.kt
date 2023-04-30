package dev.makuch.simplyTime.settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.makuch.simplyTime.databinding.ActivityWatchFaceConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchFaceConfigActivity : ComponentActivity() {
    private val stateHolder: WatchFaceConfigStateHolder by lazy {
        WatchFaceConfigStateHolder(
            lifecycleScope,
            this@WatchFaceConfigActivity
        )
    }

    private lateinit var binding: ActivityWatchFaceConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        binding = ActivityWatchFaceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable widgets until data loads and values are set.
        binding.showRingSwitch.isEnabled = false
        binding.showOnAmbientSwitch.isEnabled = false

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            stateHolder.uiState
                .collect { uiState: WatchFaceConfigStateHolder.EditWatchFaceUiState ->
                    when (uiState) {
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Loading -> {
                            Log.d(TAG, "StateFlow Loading: ${uiState.message}")
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Success -> {
                            Log.d(TAG, "StateFlow Success.")
                            updateWatchFacePreview(uiState.userStylesAndPreview)
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Error -> {
                            Log.e(TAG, "Flow error: ${uiState.exception}")
                        }
                    }
                }
        }
    }

    private fun updateWatchFacePreview(
        userStylesAndPreview: WatchFaceConfigStateHolder.UserStylesAndPreview
    ) {
        Log.d(TAG, "updateWatchFacePreview: $userStylesAndPreview")

        binding.showRingSwitch.isChecked = userStylesAndPreview.showRing
        binding.showOnAmbientSwitch.isChecked = userStylesAndPreview.showOnAmbient

        binding.showRingSwitch.isEnabled = true
        binding.showOnAmbientSwitch.isEnabled = userStylesAndPreview.showRing
    }

    fun onClickComplicationButton(view: View) {
        Log.d(TAG, "onClickLeftComplicationButton() $view")
        stateHolder.setComplication()
    }

    fun onClickShowRingSwitch(view: View) {
        Log.d(TAG, "onClickDivisionRingSwitch() $view")
        stateHolder.setDivisionRing(binding.showRingSwitch.isChecked)

        binding.showOnAmbientSwitch.isEnabled = binding.showRingSwitch.isChecked
    }

    fun onClickShowOnAmbientSwitch(view: View) {
        Log.d(TAG, "onClickDivisionRingOnAmbientSwitch() $view")
        stateHolder.setDivisionRingOnAmbient(binding.showOnAmbientSwitch.isChecked)
    }

    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
