package dev.makuch.simplyTime.editor

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import dev.makuch.simplyTime.utils.COMPLICATION_ID
import dev.makuch.simplyTime.utils.SHOW_DIVISION_RING_SETTING
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

    private lateinit var showDivisionRing: UserStyleSetting.BooleanUserStyleSetting

    val uiState: StateFlow<EditWatchFaceUiState> =
        flow<EditWatchFaceUiState> {
            editorSession = EditorSession.createOnWatchEditorSession(
                activity = activity
            )

            extractsUserStyles(editorSession.userStyleSchema)

            editorSession.userStyle.collect { userStyle ->
                emit(
                    EditWatchFaceUiState.Success(
                        getConfigData(userStyle)
                    )
                )
            }
        }
            .stateIn(
                scope + Dispatchers.Main.immediate,
                SharingStarted.Eagerly,
                EditWatchFaceUiState.Loading("Initializing")
            )

    private fun extractsUserStyles(userStyleSchema: UserStyleSchema) {
        // Loops through user styles and retrieves user editable styles.
        for (setting in userStyleSchema.userStyleSettings) {
            when (setting.id.toString()) {
                SHOW_DIVISION_RING_SETTING -> {
                    showDivisionRing = setting as UserStyleSetting.BooleanUserStyleSetting
                }
            }
        }
    }

    private fun getConfigData(
        userStyle: UserStyle
    ): UserStylesAndPreview {
        Log.d(TAG, "updatesWatchFacePreview()")

        val showDivisionRingStyle =
            userStyle[showDivisionRing] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption

        Log.d(TAG, "/new values: $showDivisionRingStyle")

        return UserStylesAndPreview(
            showDivisionRingEnabled = showDivisionRingStyle.value
        )
    }

    fun setComplication() {
        scope.launch(Dispatchers.Main.immediate) {
            editorSession.openComplicationDataSourceChooser(COMPLICATION_ID)
        }
    }

    fun setDivisionRing(enabled: Boolean) {
        setUserStyleOption(
            showDivisionRing,
            UserStyleSetting.BooleanUserStyleSetting.BooleanOption.from(enabled)
        )
    }

    private fun setUserStyleOption(
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

    sealed class EditWatchFaceUiState {
        data class Success(val userStylesAndPreview: UserStylesAndPreview) : EditWatchFaceUiState()
        data class Loading(val message: String) : EditWatchFaceUiState()
        data class Error(val exception: Throwable) : EditWatchFaceUiState()
    }

    data class UserStylesAndPreview(
        val showDivisionRingEnabled: Boolean
    )

    companion object {
        private const val TAG = "WatchFaceConfigStateHolder"
    }
}
