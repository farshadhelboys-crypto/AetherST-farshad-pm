package io.github.immaghzbad.aetherst.subscription

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun PlatformSubscriptionCard() {
    val context = LocalContext.current
    var announcement by remember { mutableStateOf<Announcement?>(null) }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) {
            AnnouncementRepository.fetchLatest()
        }
        announcement = AnnouncementRepository.unseen(context, list).firstOrNull()
    }

    // کارت اشتراک (لایسنس)
    SubscriptionCard()

    // پیام همگانی از پنل ادمین
    announcement?.let { a ->
        AnnouncementDialog(a) {
            AnnouncementRepository.markSeen(context, a.id)
            announcement = null
        }
    }
}
