package dev.makuch.simplyTime.data

const val SHOW_RING_DEFAULT = true
const val SHOW_ON_AMBIENT_DEFAULT = true

data class SettingsData(
    val showRing: Boolean = SHOW_RING_DEFAULT,
    val showOnAmbient: Boolean = SHOW_ON_AMBIENT_DEFAULT
)
