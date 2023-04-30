package dev.makuch.simplyTime.utils

import android.content.Context
import android.graphics.RectF
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import dev.makuch.simplyTime.R

private const val COMPLICATIONS_TOP_BOUND = 0.16f
private const val COMPLICATIONS_BOTTOM_BOUND = 0.36f
private const val COMPLICATION_LEFT_BOUND = 0.255f
private const val COMPLICATION_RIGHT_BOUND = 0.455f

internal const val COMPLICATION_ID = 100

sealed class ComplicationConfig(val id: Int, val supportedTypes: List<ComplicationType>) {
    object Complication : ComplicationConfig(
        COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )
}

fun createComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    drawableId: Int = R.drawable.complication_style
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, drawableId)!!,
                watchState,
                listener
            )
        }

    val complication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Complication.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Complication.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                COMPLICATION_LEFT_BOUND,
                COMPLICATIONS_TOP_BOUND,
                COMPLICATION_RIGHT_BOUND,
                COMPLICATIONS_BOTTOM_BOUND
            )
        )
    ).build()

    return ComplicationSlotsManager(
        listOf(complication),
        currentUserStyleRepository
    )
}
