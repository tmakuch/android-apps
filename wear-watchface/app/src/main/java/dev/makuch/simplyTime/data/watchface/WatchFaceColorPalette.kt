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
package dev.makuch.simplyTime.data.watchface

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import dev.makuch.simplyTime.R

/**
 * Color resources and drawable id needed to render the watch face. Translated from
 * [ColorStyleIdAndResourceIds] constant ids to actual resources with context at run time.
 *
 * This is only needed when the watch face is active.
 *
 * Note: We do not use the context to generate a [ComplicationDrawable] from the
 * complicationStyleDrawableId (representing the style), because a new, separate
 * [ComplicationDrawable] is needed for each complication. Because the renderer will loop through
 * all the complications and there can be more than one, this also allows the renderer to create
 * as many [ComplicationDrawable]s as needed.
 */
data class WatchFaceColorPalette(
    val activePrimaryColor: Int,
    val activeSecondaryColor: Int,
    val activeBackgroundColor: Int,
    val activeOuterElementColor: Int,
    @DrawableRes val complicationStyleDrawableId: Int,
    val ambientPrimaryColor: Int,
    val ambientSecondaryColor: Int,
    val ambientBackgroundColor: Int,
    val ambientOuterElementColor: Int
) {
    companion object {
        /**
         * Converts [ColorStyleIdAndResourceIds] to [WatchFaceColorPalette].
         */
        fun getWatchFaceColorPalette(
            context: Context,
        ): WatchFaceColorPalette {
            return WatchFaceColorPalette(
                // Active colors
                activePrimaryColor = context.getColor(R.color.white_primary_color),
                activeSecondaryColor = context.getColor(R.color.white_secondary_color),
                activeBackgroundColor = context.getColor(R.color.white_background_color),
                activeOuterElementColor = context.getColor(R.color.white_outer_element_color),

                // Complication color style
                complicationStyleDrawableId = R.drawable.complication_white_style,
                // Ambient colors
                ambientPrimaryColor = context.getColor(R.color.ambient_primary_color),
                ambientSecondaryColor = context.getColor(R.color.ambient_secondary_color),
                ambientBackgroundColor = context.getColor(R.color.ambient_background_color),
                ambientOuterElementColor = context.getColor(R.color.ambient_outer_element_color)
            )
        }
    }
}
