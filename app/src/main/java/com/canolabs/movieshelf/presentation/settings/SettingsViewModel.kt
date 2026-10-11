package com.canolabs.movieshelf.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import com.canolabs.movieshelf.domain.usecase.ObserveSettingsUseCase
import com.canolabs.movieshelf.domain.usecase.SetTextSizeUseCase
import com.canolabs.movieshelf.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the persisted settings and changes them. The persisted values are the only state. */
class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setTextSize: SetTextSizeUseCase,
) : ViewModel() {

    /** `null` until the settings have been read once, so the UI never renders with wrong defaults. */
    val settings: StateFlow<AppSettings?> =
        observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onTextSizeSelected(size: TextSize) {
        viewModelScope.launch { setTextSize(size) }
    }
}
