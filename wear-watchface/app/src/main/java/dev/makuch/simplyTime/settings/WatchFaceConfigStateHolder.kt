package dev.makuch.simplyTime.settings

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import dev.makuch.simplyTime.data.SettingsProps
import dev.makuch.simplyTime.data.SettingsUIState
import dev.makuch.simplyTime.utils.COMPLICATION_ID
import dev.makuch.simplyTime.utils.SHOW_ON_AMBIENT_SETTING
import dev.makuch.simplyTime.utils.SHOW_RING_SETTING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class WatchFaceConfigStateHolder(
    private val scope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private lateinit var editorSession: EditorSession

    private lateinit var showRing: UserStyleSetting.BooleanUserStyleSetting
    private lateinit var showOnAmbient: UserStyleSetting.BooleanUserStyleSetting

    val uiState: StateFlow<SettingsUIState> =
        flow<SettingsUIState> {
            editorSession = EditorSession.createOnWatchEditorSession(
                activity = activity
            )

            initSettingsPropsOnClass(editorSession.userStyleSchema)

            editorSession.userStyle.collect { userStyle ->
                emit(
                    SettingsUIState.Success(
                        getCurrentSettingsProps(userStyle)
                    )
                )
            }
        }
            .stateIn(
                scope + Dispatchers.Main.immediate,
                SharingStarted.Eagerly,
                SettingsUIState.Loading("Initializing")
            )

    private fun initSettingsPropsOnClass(userStyleSchema: UserStyleSchema) {
        for (setting in userStyleSchema.userStyleSettings) {
            when (setting.id.toString()) {
                SHOW_RING_SETTING -> {
                    showRing = setting as UserStyleSetting.BooleanUserStyleSetting
                }
                SHOW_ON_AMBIENT_SETTING -> {
                    showOnAmbient = setting as UserStyleSetting.BooleanUserStyleSetting
                }
            }
        }
    }

    private fun getCurrentSettingsProps(
        userStyle: UserStyle
    ): SettingsProps {
        Log.d(TAG, "updatesWatchFacePreview()")

        val showDivisionRingStyle =
            userStyle[showRing] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption

        val showDivisionRingOnAmbientStyle =
            userStyle[showOnAmbient] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption

        Log.d(TAG, "/new values: $showDivisionRingStyle")

        return SettingsProps(
            showRing = showDivisionRingStyle.value,
            showOnAmbient = showDivisionRingOnAmbientStyle.value
        )
    }

    fun setComplication() {
        scope.launch(Dispatchers.Main.immediate) {
            editorSession.openComplicationDataSourceChooser(COMPLICATION_ID)
        }
    }

    fun setShowRing(enabled: Boolean) {
        setSettingsProp(
            showRing,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    fun setShowOnAmbient(enabled: Boolean) {
        setSettingsProp(
            showOnAmbient,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    private fun setSettingsProp(
        userStyleSetting: UserStyleSetting,
        userStyleOption: UserStyleSetting.Option
    ) {
        Log.d(TAG, "setUserStyleOption()")
        Log.d(TAG, "\tuserStyleSetting: $userStyleSetting")
        Log.d(TAG, "\tuserStyleOption: $userStyleOption")

        val mutableUserStyle = editorSession.userStyle.value.toMutableUserStyle()
        mutableUserStyle[userStyleSetting] = userStyleOption
        editorSession.userStyle.value = mutableUserStyle.toUserStyle()
    }

    companion object {
        private const val TAG = "WatchFaceConfigStateHolder"
    }
}
