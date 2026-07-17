package com.gps19.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * MainViewModelFactory: Factory for manual ViewModel injection.
 * July.17.00:
 * - Issue #526: Performance Hardening. Now pulls pre-configured lazy UseCases 
 *   from AppContainer to prevent Main thread spikes.
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
                homePointUseCase = container.homePointUseCase,
                dashboardUseCase = container.dashboardUseCase,
                navigationUseCase = container.navigationUseCase,
                settingsUseCase = container.settingsUseCase,
                telemetryUseCase = container.telemetryUseCase,
                stateSubscriptionUseCase = container.stateSubscriptionUseCase,
                sessionUseCase = container.sessionUseCase,
                behaviorUseCase = container.behaviorUseCase,
                alertUseCase = container.alertUseCase,
                mapUseCase = container.mapUseCase,
                timeProvider = container.timeProvider,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
