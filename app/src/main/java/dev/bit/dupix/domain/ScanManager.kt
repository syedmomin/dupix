package dev.bit.dupix.domain

import dev.bit.dupix.data.repository.ScanConfig
import dev.bit.dupix.data.repository.ScanRepository
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.domain.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the current/last scan. The UI submits a config, the
 * foreground [dev.bit.dupix.service.ScanService] drives [execute], and every screen
 * observes [state]. Held as a singleton so state survives navigation and process-scoped
 * service restarts within a session.
 */
@Singleton
class ScanManager @Inject constructor(
    private val scanRepository: ScanRepository,
) {
    private val _state = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val state: StateFlow<ScanProgress> = _state.asStateFlow()

    @Volatile private var pendingConfig: ScanConfig? = null

    /** The result of the most recent successful scan, or null. */
    val lastResult: ScanResult?
        get() = (_state.value as? ScanProgress.Done)?.result

    val isRunning: Boolean
        get() = _state.value is ScanProgress.Starting ||
            _state.value is ScanProgress.Enumerating ||
            _state.value is ScanProgress.Hashing

    /** Called by the UI before starting the service. */
    fun submit(config: ScanConfig) {
        pendingConfig = config
        _state.value = ScanProgress.Starting
    }

    /** Driven by the foreground service. Runs the scan and publishes progress. */
    suspend fun execute() {
        val config = pendingConfig ?: return
        scanRepository.scan(config)
            .flowOn(Dispatchers.IO)
            .catch { e -> _state.value = ScanProgress.Failed(e.message ?: "Scan failed") }
            .collect { progress -> _state.value = progress }
    }

    fun reset() {
        _state.value = ScanProgress.Idle
        pendingConfig = null
    }
}
