package dev.makuch.simplyTime.settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.makuch.simplyTime.data.SettingsProps
import dev.makuch.simplyTime.data.SettingsUIState
import dev.makuch.simplyTime.databinding.SettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchFaceConfigActivity : ComponentActivity() {
    private val stateHolder: WatchFaceConfigStateHolder by lazy {
        WatchFaceConfigStateHolder(
            lifecycleScope,
            this@WatchFaceConfigActivity
        )
    }

    private lateinit var uiRef: SettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        uiRef = SettingsBinding.inflate(layoutInflater)
        setContentView(uiRef.root)

        uiRef.showRingSwitch.isEnabled = false
        uiRef.showOnAmbientSwitch.isEnabled = false

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            stateHolder.uiState
                .collect { uiState: SettingsUIState ->
                    when (uiState) {
                        is SettingsUIState.Loading -> {
                            Log.d(TAG, "StateFlow Loading: ${uiState.message}")
                        }
                        is SettingsUIState.Success -> {
                            Log.d(TAG, "StateFlow Success.")
                            updateSettingsUI(uiState.props)
                        }
                    }
                }
        }
    }

    private fun updateSettingsUI(
        props: SettingsProps
    ) {
        Log.d(TAG, "updateSettingsUI: $props")

        uiRef.showRingSwitch.isChecked = props.showRing
        uiRef.showOnAmbientSwitch.isChecked = props.showOnAmbient

        uiRef.showRingSwitch.isEnabled = true
        uiRef.showOnAmbientSwitch.isEnabled = props.showRing
    }

    fun handleComplicationSettingClick(view: View) {
        Log.d(TAG, "handleComplicationSettingClick() $view")
        stateHolder.setComplication()
    }

    fun handleShowRingSettingClick(view: View) {
        Log.d(TAG, "handleShowRingSettingClick() $view")
        stateHolder.setShowRing(uiRef.showRingSwitch.isChecked)

        uiRef.showOnAmbientSwitch.isEnabled = uiRef.showRingSwitch.isChecked
    }

    fun handleShowOnAmbientSettingClick(view: View) {
        Log.d(TAG, "handleShowOnAmbientSettingClick() $view")
        stateHolder.setShowOnAmbient(uiRef.showOnAmbientSwitch.isChecked)
    }

    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
