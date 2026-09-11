package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Announcement(val id: Long, val title: String, val body: String, val createdAt: Long)

object AnnouncementRepository {
    private const val API = "https://aetherst-license-api.farshadhelboys.workers.dev"
    private const val PREFS = "announcement_prefs"
    private const val KEY_SEEN = "seen_ids"

    suspend fun fetchLatest(): List<Announcement> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$API/v1/announcements?limit=5").openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val json = JSONObject(text)
            if (!json.optBoolean("ok", false)) return@withContext emptyList()
            val arr = json.optJSONArray("announcements") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Announcement(
                            id = o.optLong("id"),
                            title = o.optString("title"),
                            body = o.optString("body"),
                            createdAt = o.optLong("created_at")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun unseen(context: Context, list: List<Announcement>): List<Announcement> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
        return list.filter { it.id.toString() !in seen }
    }

    fun markSeen(context: Context, id: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet(KEY_SEEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        seen.add(id.toString())
        prefs.edit().putStringSet(KEY_SEEN, seen).apply()
    }
}

/**
 * در Dashboard یا MainScreen:
 *
 * var ann by remember { mutableStateOf<Announcement?>(null) }
 * val ctx = LocalContext.current
 * LaunchedEffect(Unit) {
 *   val list = AnnouncementRepository.fetchLatest()
 *   ann = AnnouncementRepository.unseen(ctx, list).firstOrNull()
 * }
 * ann?.let { a ->
 *   AnnouncementDialog(a) {
 *     AnnouncementRepository.markSeen(ctx, a.id)
 *     ann = null
 *   }
 * }
 */
@Composable
fun AnnouncementDialog(announcement: Announcement, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "📢 ${announcement.title}",
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = announcement.body,
                    color = AppPalette.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)
                ) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                }
            }
        }
    }
}
