package dev.makuch.simplyTime.utils

import android.graphics.Canvas
import android.graphics.Rect
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.RenderParameters
import dev.makuch.simplyTime.data.Paints
import java.time.LocalTime
import java.time.ZonedDateTime



fun drawComplications(canvas: Canvas, renderParameters: RenderParameters, zonedDateTime: ZonedDateTime, complicationSlotsManager: ComplicationSlotsManager) {
    for ((_, complication) in complicationSlotsManager.complicationSlots) {
        if (complication.enabled) {
            complication.render(canvas, zonedDateTime, renderParameters)
        }
    }
}

fun drawMainTime(
    canvas: Canvas,
    bounds: Rect,
    localTime: LocalTime,
    paints: Paints,
    heightOffset: Float,
    isAmbient: Boolean
) {
    val minutes = String.format("%02d", localTime.minute)
    val hours = localTime.hour.toString()

    val isAnyOtherHalfOfSecond = localTime.nano > 500000000

    val wholeTimeOffset = paints.textPaint.measureText("$hours:$minutes") / 2
    val hoursWidth = paints.textPaint.measureText(hours)
    val colonWidth = paints.textPaint.measureText(":")

    val fontToUse =
        if (isAmbient) paints.ambientTextPaint else paints.textPaint

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

fun drawSeconds(
    canvas: Canvas,
    bounds: Rect,
    localTime: LocalTime,
    paints: Paints,
    heightOffset: Float,
    margin: Float
) {
    val seconds = String.format("%02d", localTime.second)

    val isAnyOtherHalfOfSecond = localTime.nano > 500000000


    val yPosition =
        bounds.centerY()
            .toFloat() + heightOffset - margin - paints.secondaryTextPaint.textSize
    val secondaryColonWidth = paints.secondaryTextPaint.measureText(":")

    if (isAnyOtherHalfOfSecond) {
        canvas.drawText(
            ":",
            bounds.centerX().toFloat(),
            yPosition,
            paints.secondaryTextPaint
        )
    }

    canvas.drawText(
        seconds,
        bounds.centerX() + secondaryColonWidth,
        yPosition,
        paints.secondaryTextPaint
    )
}

fun drawDate(
    canvas: Canvas,
    bounds: Rect,
    zonedDateTime: ZonedDateTime,
    paints: Paints,
    heightOffset: Float
) {
    val dayOfMonth = zonedDateTime.dayOfMonth
    val dayOfWeek = mapDayOfWeek(zonedDateTime.dayOfWeek)
    val text = "$dayOfWeek.$dayOfMonth"

    canvas.drawText(
        text,
        bounds.width() * 0.3f,
        bounds.centerY().toFloat() + heightOffset + paints.tertiaryTextPaint.textSize,
        paints.tertiaryTextPaint
    )
}


fun drawRing(
    canvas: Canvas,
    bounds: Rect,
    paints: Paints,
) {
    canvas.drawCircle(
        bounds.exactCenterX(),
        bounds.exactCenterY(),
        bounds.width().toFloat() / 2,
        paints.divisionRingPaint
    )
}
