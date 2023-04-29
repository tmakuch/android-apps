package dev.makuch.simplyTime.data.watchface

import android.content.Context
import android.graphics.Paint
import dev.makuch.simplyTime.R

data class WatchFacePaints(
    val textPaint: Paint,
    val ambientTextPaint: Paint,
    val secondaryTextPaint: Paint,
    val tertiaryTextPaint: Paint,
    val divisionRingPaint: Paint
) {
    companion object {
        fun getPaints(
            context: Context,
            watchFaceColors: WatchFaceColorPalette
        ): WatchFacePaints {
            return WatchFacePaints(
                textPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = context.resources.getDimensionPixelSize(R.dimen.font_size).toFloat()
                    color = watchFaceColors.activeFrontendColor
                    typeface = context.resources.getFont(R.font.digital)
                },
                ambientTextPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = context.resources.getDimensionPixelSize(R.dimen.font_size).toFloat()
                    color = watchFaceColors.activeFrontendColor
                    typeface = context.resources.getFont(R.font.digital_empty)
                },
                secondaryTextPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = context.resources.getDimensionPixelSize(R.dimen.secondary_font_size).toFloat()
                    color = watchFaceColors.activeFrontendColor
                    typeface = context.resources.getFont(R.font.digital)
                },
                tertiaryTextPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = context.resources.getDimensionPixelSize(R.dimen.tertiary_font_size).toFloat()
                    color = watchFaceColors.activeFrontendColor
                    typeface = context.resources.getFont(R.font.digital)
                },
                divisionRingPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = watchFaceColors.divisionRingColor
                    strokeWidth = 20f //half is of the screen
                }
            )
        }
    }
}
