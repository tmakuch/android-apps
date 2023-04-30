package dev.makuch.simplyTime.utils

import android.content.Context
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import dev.makuch.simplyTime.R
import dev.makuch.simplyTime.data.SHOW_RING_DEFAULT
import dev.makuch.simplyTime.data.SHOW_ON_AMBIENT_DEFAULT

const val SHOW_RING_SETTING = "show_ring_setting"
const val SHOW_ON_AMBIENT_SETTING = "show_on_ambient_setting"

fun createUserStyleSchema(context: Context): UserStyleSchema {
    val showRingSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(SHOW_RING_SETTING),
        context.resources,
        R.string.show_div_ring_setting,
        R.string.show_div_ring_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        SHOW_RING_DEFAULT
    )

    val showOnAmbientSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(SHOW_ON_AMBIENT_SETTING),
        context.resources,
        R.string.show_div_ring_on_ambient_setting,
        R.string.show_div_ring_on_ambient_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        SHOW_ON_AMBIENT_DEFAULT
    )

    return UserStyleSchema(
        listOf(
            showOnAmbientSetting,
            showRingSetting
        )
    )
}
