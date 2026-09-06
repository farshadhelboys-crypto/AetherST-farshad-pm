package io.github.immaghzbad.aetherst.platform

import io.github.immaghzbad.aetherst.subscription.SubscriptionRepository
import kotlinx.coroutines.*

actual class LicenseGate actual constructor(context: PlatformContext) {
    private val repo = SubscriptionRepository(context.context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitor: Job? = null

    actual suspend fun isConnectionAllowed(): Boolean = repo.isConnectionAllowed()

    actual fun startMonitoring(onExpired: () -> Unit) {
        if (monitor?.isActive == true) return
        monitor = scope.launch {
            while (isActive) {
                delay(10 * 60 * 1000L)
                if (!repo.isConnectionAllowed()) withContext(Dispatchers.Main) { onExpired() }
            }
        }
    }

    actual fun stopMonitoring() { monitor?.cancel(); monitor = null }
}
