package dev.bit.dupix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bit.dupix.R
import dev.bit.dupix.domain.ScanManager
import dev.bit.dupix.domain.model.ScanProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Keeps the scan alive in the background with a progress notification. Drives
 * [ScanManager.execute] and mirrors its state into the notification, stopping itself when
 * the scan reaches a terminal state.
 */
@AndroidEntryPoint
class ScanService : Service() {

    @Inject lateinit var scanManager: ScanManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var runJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Preparing scan…", indeterminate = true))

        // Mirror progress into the notification.
        scope.launch {
            scanManager.state.collect { progress ->
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, notificationFor(progress))
                if (progress is ScanProgress.Done || progress is ScanProgress.Failed) {
                    stopForegroundAndSelf()
                }
            }
        }

        if (runJob?.isActive != true) {
            runJob = scope.launch { scanManager.execute() }
        }
        return START_NOT_STICKY
    }

    private fun notificationFor(progress: ScanProgress): Notification = when (progress) {
        is ScanProgress.Enumerating ->
            buildNotification("Scanning ${progress.category.label}… ${progress.filesFound} files", true)
        is ScanProgress.Hashing ->
            buildNotification("Analyzing ${progress.processed}/${progress.total}", false, progress.processed, progress.total)
        is ScanProgress.Done ->
            buildNotification("Scan complete", indeterminate = false, ongoing = false)
        is ScanProgress.Failed ->
            buildNotification("Scan failed", indeterminate = false, ongoing = false)
        else -> buildNotification("Preparing scan…", indeterminate = true)
    }

    private fun buildNotification(
        text: String,
        indeterminate: Boolean,
        progress: Int = 0,
        max: Int = 0,
        ongoing: Boolean = true,
    ): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(text)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOnlyAlertOnce(true)
        .setOngoing(ongoing)
        .apply {
            if (max > 0) setProgress(max, progress, indeterminate)
            else if (indeterminate) setProgress(0, 0, true)
        }
        .build()

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_scan),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "scan"
        private const val NOTIF_ID = 1001

        /** Starts the scan service. Call [ScanManager.submit] first. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScanService::class.java))
        }
    }
}
