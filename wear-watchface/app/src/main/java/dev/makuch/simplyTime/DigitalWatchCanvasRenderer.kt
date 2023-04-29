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
import dev.makuch.simplyTime.data.watchface.WatchFaceColorPalette
import dev.makuch.simplyTime.data.watchface.WatchFaceData
import dev.makuch.simplyTime.data.watchface.WatchFacePaints
import dev.makuch.simplyTime.utils.SHOW_DIVISION_RING_SETTING
import dev.makuch.simplyTime.utils.mapDayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
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

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val fontHeightOffsetModificator: Float = (1 / 2.8).toFloat()
    private val topMargin: Float =
        context.resources.getDimensionPixelSize(R.dimen.top_margin).toFloat()

    private var watchFaceData: WatchFaceData = WatchFaceData()
    private var watchFaceColors = WatchFaceColorPalette.getWatchFaceColorPalette(
        context,
    )
    private var watchFacePaints: WatchFacePaints = WatchFacePaints.getPaints(context, watchFaceColors)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
    }

    override suspend fun createSharedAssets(): DigitalSharedAssets {
        return DigitalSharedAssets()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newWatchFaceData: WatchFaceData = watchFaceData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                SHOW_DIVISION_RING_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        showDivisionRing = booleanValue.value
                    )
                }
            }
        }

        // Only updates if something changed.
        if (watchFaceData != newWatchFaceData) {
            watchFaceData = newWatchFaceData

            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    watchFaceColors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
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
        canvas.drawColor(watchFaceColors.backgroundColor)

        val localTime = zonedDateTime.toLocalTime()
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val heightOffset = watchFacePaints.textPaint.textSize * fontHeightOffsetModificator

        drawMainTime(canvas, bounds, localTime, isAmbient, heightOffset)
        if (!isAmbient) {
            drawComplications(canvas, zonedDateTime)
            drawSeconds(canvas, bounds, localTime, heightOffset)
            drawDate(canvas, bounds, zonedDateTime, heightOffset)
        }
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawMainTime(
        canvas: Canvas,
        bounds: Rect,
        localTime: LocalTime,
        isAmbient: Boolean,
        heightOffset: Float
    ) {
        val minutes = String.format("%02d", localTime.minute)
        val hours = localTime.hour.toString()

        val isAnyOtherHalfOfSecond = localTime.nano > 500000000

        val wholeTimeOffset = watchFacePaints.textPaint.measureText("$hours:$minutes") / 2
        val hoursWidth = watchFacePaints.textPaint.measureText(hours)
        val colonWidth = watchFacePaints.textPaint.measureText(":")

        val fontToUse = if (isAmbient) watchFacePaints.ambientTextPaint else watchFacePaints.textPaint

        canvas.drawText(
            hours,
            bounds.centerX() - wholeTimeOffset,
            bounds.centerY().toFloat() + heightOffset,
            fontToUse
        )
        if (isAmbient || isAnyOtherHalfOfSecond) {
            canvas.drawText(
                ":",
                bounds.centerX() - wholeTimeOffset + hoursWidth,
                bounds.centerY().toFloat() + heightOffset,
                fontToUse
            )
        }
        canvas.drawText(
            minutes,
            bounds.centerX() - wholeTimeOffset + hoursWidth + colonWidth,
            bounds.centerY().toFloat() + heightOffset,
            fontToUse
        )
    }

    private fun drawSeconds(
        canvas: Canvas,
        bounds: Rect,
        localTime: LocalTime,
        heightOffset: Float
    ) {
        val seconds = String.format("%02d", localTime.second)

        val isAnyOtherHalfOfSecond = localTime.nano > 500000000


        val yPosition =
            bounds.centerY().toFloat() + heightOffset - topMargin - watchFacePaints.secondaryTextPaint.textSize
        val secondaryColonWidth = watchFacePaints.secondaryTextPaint.measureText(":")

        if (isAnyOtherHalfOfSecond) {
            canvas.drawText(
                ":",
                bounds.centerX().toFloat(),
                yPosition,
                watchFacePaints.secondaryTextPaint
            )
        }

        canvas.drawText(
            seconds,
            bounds.centerX() + secondaryColonWidth,
            yPosition,
            watchFacePaints.secondaryTextPaint
        )
    }

    private fun drawDate(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        heightOffset: Float
    ) {
        val dayOfMonth = zonedDateTime.dayOfMonth
        val dayOfWeek = mapDayOfWeek(zonedDateTime.dayOfWeek)
        val text = "$dayOfWeek.$dayOfMonth"

        canvas.drawText(
            text,
            bounds.width() * 0.3f,
            bounds.centerY().toFloat() + heightOffset + watchFacePaints.tertiaryTextPaint.textSize,
            watchFacePaints.tertiaryTextPaint
        )
    }


//    private fun drawDivisionRing(
//        canvas: Canvas,
//        bounds: Rect,
//        radiusFraction: Float,
//        gapBetweenOuterCircleAndBorderFraction: Float
//    ) {
//        // X and Y coordinates of the center of the circle.
//        val centerX = 0.5f * bounds.width().toFloat()
//        val centerY = bounds.width() * (gapBetweenOuterCircleAndBorderFraction + radiusFraction)
//
//        canvas.drawCircle(
//            centerX,
//            centerY,
//            radiusFraction * bounds.width(),
//            watchFacePaints.divisionRingPaint
//        )
//    }

    companion object {
        private const val TAG = "AnalogWatchCanvasRenderer"
    }
}
