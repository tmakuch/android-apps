/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.makuch.simplyTime.editor

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.editor.EditorSession
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer

import dev.makuch.simplyTime.utils.COMPLICATION_ID
import dev.makuch.simplyTime.utils.SHOW_DIVISION_RING_SETTING

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.yield

class WatchFaceConfigStateHolder(
    private val scope: CoroutineScope,
    private val activity: ComponentActivity
) {
    private lateinit var editorSession: EditorSession

    // Keys from Watch Face Data Structure
    private lateinit var showDivisionRing: UserStyleSetting.BooleanUserStyleSetting

    val uiState: StateFlow<EditWatchFaceUiState> =
        flow<EditWatchFaceUiState> {
            editorSession = EditorSession.createOnWatchEditorSession(
                activity = activity
            )

            extractsUserStyles(editorSession.userStyleSchema)

            emitAll(
                combine(
                    editorSession.userStyle,
                    editorSession.complicationsPreviewData
                ) { userStyle, complicationsPreviewData ->
                    yield()
                    EditWatchFaceUiState.Success(
                        createWatchFacePreview(userStyle, complicationsPreviewData)
                    )
                }
            )
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

    private fun createWatchFacePreview(
        userStyle: UserStyle,
        complicationsPreviewData: Map<Int, ComplicationData>
    ): UserStylesAndPreview {
        Log.d(TAG, "updatesWatchFacePreview()")

        val bitmap = editorSession.renderWatchFaceToBitmap( //TODO drop this
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.RED, // Red complication highlight.
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            editorSession.previewReferenceInstant,
            complicationsPreviewData
        )

        val showDivisionRingStyle =
            userStyle[showDivisionRing] as UserStyleSetting.BooleanUserStyleSetting.BooleanOption

        Log.d(TAG, "/new values: $showDivisionRingStyle")

        return UserStylesAndPreview(
            showDivisionRingEnabled = showDivisionRingStyle.value
        )
    }

    fun setComplication(complicationLocation: Int) {
        val complicationSlotId = when (complicationLocation) {
            COMPLICATION_ID -> {
                COMPLICATION_ID
            }
            else -> {
                return
            }
        }
        scope.launch(Dispatchers.Main.immediate) {
            editorSession.openComplicationDataSourceChooser(complicationSlotId)
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
