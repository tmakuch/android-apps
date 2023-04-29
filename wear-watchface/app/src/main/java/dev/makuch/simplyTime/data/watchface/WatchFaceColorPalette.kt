package dev.makuch.simplyTime.data.watchface

import android.content.Context
import androidx.annotation.DrawableRes
import dev.makuch.simplyTime.R

data class WatchFaceColorPalette(
    val backgroundColor: Int,
    val activeFrontendColor: Int,
    val ambientFrontendColor: Int,
    val divisionRingColor: Int,
    @DrawableRes val complicationStyleDrawableId: Int,
) {
    companion object {
        fun getWatchFaceColorPalette(
            context: Context,
        ): WatchFaceColorPalette {
            return WatchFaceColorPalette(
                backgroundColor = context.getColor(R.color.background_color),
                activeFrontendColor = context.getColor(R.color.primary_color),
                ambientFrontendColor = context.getColor(R.color.ambient_primary_color),
                divisionRingColor = context.getColor(R.color.division_ring_color),
                complicationStyleDrawableId = R.drawable.complication_style,
            )
        }
    }
}
