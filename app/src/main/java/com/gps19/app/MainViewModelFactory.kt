package com.gps19.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * MainViewModelFactory: Standard factory for manual ViewModel injection.
 * Part of Issue #503: Hilt Removal.
 */
class MainViewModelFactory(
    private val context: Context,
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                repository = container.mainRepository,
                logManager = container.logManager,
                systemStatusProvider = container.systemStatusProvider,
                homePointUseCase = HomePointUseCase(container.mainRepository),
                dashboardUseCase = DashboardUseCase(),
                navigationUseCase = NavigationUseCase(),
                settingsUseCase = SettingsUseCase(
                    container.mainRepository,
                    container.settingsRepository,
                    container.timeProvider,
                    container.logManager
                ),
                telemetryUseCase = TelemetryUseCase(container.timeProvider),
                stateSubscriptionUseCase = container.stateSubscriptionUseCase,
                sessionUseCase = SessionUseCase(container.mainRepository, container.timeProvider),
                behaviorUseCase = BehaviorUseCase(),
                alertUseCase = AlertUseCase(
                    container.mainRepository,
                    container.timeProvider,
                    container.logManager
                ),
                mapUseCase = MapUseCase(),
                timeProvider = container.timeProvider,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
