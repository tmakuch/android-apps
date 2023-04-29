package dev.makuch.simplyTime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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
import dev.makuch.simplyTime.utils.SHOW_DIVISION_RING_SETTING
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
    private val textMargin: Float = context.resources.getDimensionPixelSize(R.dimen.text_margin).toFloat()

    // Represents all data needed to render the watch face. All value defaults are constants. Only
    // three values are changeable by the user (color scheme, ticks being rendered, and length of
    // the minute arm). Those dynamic values are saved in the watch face APIs and we update those
    // here (in the renderer) through a Kotlin Flow.
    private var watchFaceData: WatchFaceData = WatchFaceData()

    // Converts resource ids into Colors and ComplicationDrawable.
    private var watchFaceColors = WatchFaceColorPalette.getWatchFaceColorPalette(
        context,
    )

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.font_size).toFloat()
        color = watchFaceColors.activeFrontendColor
        typeface = context.resources.getFont(R.font.digital)
    }

    private val ambientTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.font_size).toFloat()
        color = watchFaceColors.activeFrontendColor
        typeface = context.resources.getFont(R.font.digital_empty)
    }

    private val secondaryTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.secondary_font_size).toFloat()
        color = watchFaceColors.activeFrontendColor
        typeface = context.resources.getFont(R.font.digital)
    }

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

        // CanvasComplicationDrawable already obeys rendererParameters.
        drawComplications(canvas, zonedDateTime)
        drawHours(canvas, bounds, zonedDateTime)

//        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) { //TODO ten if jest do wywalenia?
//            drawClockHands(canvas, bounds, zonedDateTime)
//        }
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawHours(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val localTime = zonedDateTime.toLocalTime()
        val seconds = java.lang.String.format("%02d", localTime.second)
        val minutes = java.lang.String.format("%02d", localTime.minute)
        val hours = localTime.hour.toString()

        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val isAnyOtherHalfOfSecond = localTime.nano > 500000000

        val wholeTimeOffset = textPaint.measureText("$hours:$minutes") / 2
        val hoursWidth = textPaint.measureText(hours)
        val colonWidth = textPaint.measureText(":")
        val heightOffset = textPaint.textSize * fontHeightOffsetModificator

        val fontToUse = if (isAmbient) ambientTextPaint else textPaint

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
        if (!isAmbient) {
            val yPosition = bounds.centerY().toFloat() + heightOffset - textMargin - secondaryTextPaint.textSize
            val secondaryColonWidth = secondaryTextPaint.measureText(":")

            if (isAnyOtherHalfOfSecond) {
                canvas.drawText(
                    ":",
                    bounds.centerX().toFloat(),
                    yPosition,
                    secondaryTextPaint
                )
            }

            canvas.drawText(
                seconds,
                bounds.centerX() + secondaryColonWidth,
                yPosition,
                secondaryTextPaint
            )
        }
    }

//
//    /** Draws the outer circle on the top middle of the given bounds. */
//    private fun drawTopMiddleCircle(
//        canvas: Canvas,
//        bounds: Rect,
//        radiusFraction: Float,
//        gapBetweenOuterCircleAndBorderFraction: Float
//    ) {
//        outerElementPaint.style = Paint.Style.FILL_AND_STROKE
//
//        // X and Y coordinates of the center of the circle.
//        val centerX = 0.5f * bounds.width().toFloat()
//        val centerY = bounds.width() * (gapBetweenOuterCircleAndBorderFraction + radiusFraction)
//
//        canvas.drawCircle(
//            centerX,
//            centerY,
//            radiusFraction * bounds.width(),
//            outerElementPaint
//        )
//    }

    companion object {
        private const val TAG = "AnalogWatchCanvasRenderer"
    }
}
