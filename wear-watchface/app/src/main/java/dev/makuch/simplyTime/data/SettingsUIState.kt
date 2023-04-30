package dev.makuch.simplyTime.data

sealed class SettingsUIState {
    data class Success(val props: SettingsProps) : SettingsUIState()
    data class Loading(val message: String) : SettingsUIState()
}
