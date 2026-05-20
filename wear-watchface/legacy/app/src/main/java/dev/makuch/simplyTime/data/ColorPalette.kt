package dev.makuch.simplyTime.data

import android.content.Context
import androidx.annotation.DrawableRes
import dev.makuch.simplyTime.R

data class ColorPalette(
    val backgroundColor: Int,
    val activeFrontendColor: Int,
    val ambientFrontendColor: Int,
    val divisionRingColor: Int,
    @DrawableRes val complicationStyleDrawableId: Int,
) {
    companion object {
        fun getColorPalette(
            context: Context,
        ): ColorPalette {
            return ColorPalette(
                backgroundColor = context.getColor(R.color.background_color),
                activeFrontendColor = context.getColor(R.color.primary_color),
                ambientFrontendColor = context.getColor(R.color.ambient_primary_color),
                divisionRingColor = context.getColor(R.color.division_ring_color),
                complicationStyleDrawableId = R.drawable.complication_style,
            )
        }
    }
}
