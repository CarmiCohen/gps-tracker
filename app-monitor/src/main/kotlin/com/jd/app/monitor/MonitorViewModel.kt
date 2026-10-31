package com.jd.app.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jd.core.network.ForensicIdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val repository: MonitorRepository,
    private val identityManager: ForensicIdentityManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val trail: StateFlow<List<TrailEntity>> = identityManager.targetId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.getTrail(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetId = identityManager.targetId
    val selfId = identityManager.selfId

    fun setTargetId(id: String) {
        viewModelScope.launch {
            identityManager.setTargetId(id)
        }
    }

    fun clearTargetId() {
        viewModelScope.launch {
            identityManager.clearTargetId()
        }
    }
}
