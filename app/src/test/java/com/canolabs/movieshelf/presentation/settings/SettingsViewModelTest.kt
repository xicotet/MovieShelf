package com.canolabs.movieshelf.presentation.settings

import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import com.canolabs.movieshelf.domain.repository.SettingsRepository
import com.canolabs.movieshelf.domain.usecase.ObserveSettingsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.SetTextSizeUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.SetThemeModeUseCaseImpl
import com.canolabs.movieshelf.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }
        override suspend fun setTextSize(size: TextSize) = settings.update { it.copy(textSize = size) }
    }

    private val repository = FakeSettingsRepository()
    private val viewModel by lazy {
        SettingsViewModel(
            ObserveSettingsUseCaseImpl(repository),
            SetThemeModeUseCaseImpl(repository),
            SetTextSizeUseCaseImpl(repository),
        )
    }

    @Test
    fun `settings are unknown until read, then the stored values`() = runTest {
        assertNull(viewModel.settings.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }

        assertEquals(AppSettings(), viewModel.settings.value)
    }

    @Test
    fun `selections are persisted and exposed`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }

        viewModel.onThemeModeSelected(ThemeMode.Light)
        viewModel.onTextSizeSelected(TextSize.Small)

        assertEquals(AppSettings(ThemeMode.Light, TextSize.Small), repository.settings.value)
        assertEquals(AppSettings(ThemeMode.Light, TextSize.Small), viewModel.settings.value)
    }
}
