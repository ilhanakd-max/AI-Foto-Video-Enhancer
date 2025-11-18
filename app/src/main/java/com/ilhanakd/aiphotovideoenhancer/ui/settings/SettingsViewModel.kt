package com.ilhanakd.aiphotovideoenhancer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanakd.aiphotovideoenhancer.data.preferences.PreferenceRepository
import com.ilhanakd.aiphotovideoenhancer.domain.model.AppThemeOption
import com.ilhanakd.aiphotovideoenhancer.domain.model.LocaleOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PreferenceRepository) : ViewModel() {
    val language = repository.language.stateIn(viewModelScope, SharingStarted.Eagerly, LocaleOption.SYSTEM)
    val theme = repository.theme.stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeOption.SYSTEM)

    fun setLanguage(option: LocaleOption) {
        viewModelScope.launch {
            repository.setLanguage(option)
        }
    }

    fun setTheme(option: AppThemeOption) {
        viewModelScope.launch {
            repository.setTheme(option)
        }
    }
}
