package dev.makuch.simplyTime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import dev.makuch.simplyTime.data.ColorPalette
import dev.makuch.simplyTime.data.SettingsData
import dev.makuch.simplyTime.data.Paints
import dev.makuch.simplyTime.utils.SHOW_ON_AMBIENT_SETTING
import dev.makuch.simplyTime.utils.SHOW_RING_SETTING
import dev.makuch.simplyTime.utils.drawComplications
import dev.makuch.simplyTime.utils.drawDate
import dev.makuch.simplyTime.utils.drawMainTime
import dev.makuch.simplyTime.utils.drawRing
import dev.makuch.simplyTime.utils.drawSeconds
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

class DigitalWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<DigitalWatchCanvasRenderer.DigitalSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class DigitalSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val fontHeightOffsetModificator: Float = (1 / 2.8).toFloat()
    private val topMargin: Float =
        context.resources.getDimensionPixelSize(R.dimen.top_margin).toFloat()

    private var settingsData: SettingsData = SettingsData()
    private var colors = ColorPalette.getColorPalette(context)
    private var paints: Paints = Paints.getPaints(context, colors)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateSettingsData(userStyle)
            }
        }
    }

    override suspend fun createSharedAssets(): DigitalSharedAssets {
        return DigitalSharedAssets()
    }

    private fun updateSettingsData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newSettingsData: SettingsData = settingsData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                SHOW_RING_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newSettingsData = newSettingsData.copy(
                        showRing = booleanValue.value
                    )
                }

                SHOW_ON_AMBIENT_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newSettingsData = newSettingsData.copy(
                        showOnAmbient = booleanValue.value
                    )
                }
            }
        }

        if (settingsData != newSettingsData) {
            settingsData = newSettingsData

            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    colors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("DigitalWatchCanvasRenderer scope clear() request")
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
        canvas.drawColor(colors.backgroundColor)

        val localTime = zonedDateTime.toLocalTime()
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val heightOffset = paints.textPaint.textSize * fontHeightOffsetModificator

        drawMainTime(canvas, bounds, localTime, paints, heightOffset, isAmbient)

        if (settingsData.showRing && (!isAmbient || settingsData.showOnAmbient)) {
            drawRing(canvas, bounds, paints)
        }

        if (!isAmbient) {
            drawComplications(canvas, renderParameters, zonedDateTime, complicationSlotsManager)
            drawSeconds(canvas, bounds, localTime, paints, heightOffset, topMargin)
            drawDate(canvas, bounds, zonedDateTime, paints, heightOffset)
        }
    }

    companion object {
        private const val TAG = "AnalogWatchCanvasRenderer"
    }
}
