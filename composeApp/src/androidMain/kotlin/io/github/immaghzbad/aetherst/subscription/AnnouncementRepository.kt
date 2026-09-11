package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

data class Announcement(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long
)

object AnnouncementRepository {
    private const val API = "https://aetherst-license-api.farshadhelboys.workers.dev"
    private const val PREFS = "announcement_prefs"
    private const val KEY_SEEN = "seen_announcement_ids"

    suspend fun fetchLatest(limit: Int = 5): List<Announcement> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$API/v1/announcements?limit=$limit").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText().orEmpty()
            conn.disconnect()
            val json = JSONObject(text.ifBlank { "{}" })
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
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "متوجه شدم",
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.onAccent
                    )
                }
            }
        }
    }
}
