package dev.bit.dupix.domain

import dev.bit.dupix.data.repository.ScanConfig
import dev.bit.dupix.data.repository.ScanRepository
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.domain.model.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the current/last scan, held as an application-scoped
 * singleton.
 *
 * The scan runs on [scope] — an app-lifetime coroutine scope — NOT on the Activity or the
 * Service. That means:
 *  - Leaving the scan screen or backgrounding the app does not stop the scan.
 *  - The scan keeps running even if the foreground [dev.bit.dupix.service.ScanService] is
 *    restarted; the service only keeps the process alive and shows the notification.
 *  - The last result stays in memory ([state] Done) while the process lives, so results
 *    are retained across navigation/backgrounding — and are only lost when the app's
 *    process is fully killed (full close), which is the intended behavior.
 */
@Singleton
class ScanManager @Inject constructor(
    private val scanRepository: ScanRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

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

    /** Records the config and flips to Starting. Call [start] to actually run. */
    fun submit(config: ScanConfig) {
        pendingConfig = config
        _state.value = ScanProgress.Starting
    }

    /**
     * Launches the scan on the app-scoped [scope]. Idempotent: if a scan is already
     * running, this is a no-op (so a service restart won't spawn a second scan).
     */
    fun start() {
        if (job?.isActive == true) return
        val config = pendingConfig ?: return
        job = scope.launch {
            scanRepository.scan(config)
                .flowOn(Dispatchers.IO)
                .catch { e -> _state.value = ScanProgress.Failed(e.message ?: "Scan failed") }
                .collect { progress -> _state.value = progress }
        }
    }

    /** Cancels an in-progress scan (keeps any already-published result state). */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /** Clears state and any in-progress scan (e.g. to start over). */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = ScanProgress.Idle
        pendingConfig = null
    }
}
