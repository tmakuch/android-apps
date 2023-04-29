/*
 * Copyright 2020 The Android Open Source Project
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
package dev.makuch.simplyTime.utils

import android.content.Context
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import dev.makuch.simplyTime.R
import dev.makuch.simplyTime.data.watchface.SHOW_DIVISION_RING_DEFAULT

// Keys to matched content in the  the user style settings. We listen for changes to these
// values in the renderer and if new, we will update the database and update the watch face
// being rendered.
const val SHOW_DIVISION_RING_SETTING = "show_division_ring_setting"

/*
 * Creates user styles in the settings activity associated with the watch face, so users can
 * edit different parts of the watch face. In the renderer (after something has changed), the
 * watch face listens for a flow from the watch face API data layer and updates the watch face.
 */
fun createUserStyleSchema(context: Context): UserStyleSchema {
    val showDivisionRingSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(SHOW_DIVISION_RING_SETTING),
        context.resources,
        R.string.show_div_ring_setting,
        R.string.show_div_ring_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        SHOW_DIVISION_RING_DEFAULT
    )

    return UserStyleSchema(
        listOf(
            showDivisionRingSetting,
        )
    )
}
