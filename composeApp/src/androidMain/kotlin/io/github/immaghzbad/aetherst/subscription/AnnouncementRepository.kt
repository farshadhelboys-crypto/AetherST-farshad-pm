package io.github.immaghzbad.aetherst.subscription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

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

    private val URL_REGEX: Pattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>"']+|t\.me/[^\s<>"']+|telegram\.me/[^\s<>"']+)"""
    )

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

    fun normalizeUrl(raw: String): String {
        val t = raw.trim().trimEnd('.', ',', ')', ']', '»', '"', '\'')
        return when {
            t.startsWith("http://", true) || t.startsWith("https://", true) -> t
            t.startsWith("www.", true) -> "https://$t"
            t.startsWith("t.me/", true) || t.startsWith("telegram.me/", true) -> "https://$t"
            else -> "https://$t"
        }
    }

    fun extractUrls(text: String): List<String> {
        val m = URL_REGEX.matcher(text)
        val out = mutableListOf<String>()
        while (m.find()) out.add(m.group())
        return out
    }
}

@Composable
fun LinkifiedBodyText(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkColor = AppPalette.accent
    val normalColor = AppPalette.textSecondary

    val annotated = remember(text) {
        buildAnnotatedString {
            val matcher = Pattern.compile(
                """(?i)\b((?:https?://|www\.)[^\s<>"']+|t\.me/[^\s<>"']+|telegram\.me/[^\s<>"']+)"""
            ).matcher(text)
            var last = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start > last) {
                    withStyle(SpanStyle(color = normalColor)) {
                        append(text.substring(last, start))
                    }
                }
                val raw = matcher.group()
                val url = AnnouncementRepository.normalizeUrl(raw)
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(raw)
                }
                pop()
                last = end
            }
            if (last < text.length) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(last))
                }
            }
        }
    }

    var layoutResult: TextLayoutResult? = null

    ClickableText(
        text = annotated,
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures(
                onLongPress = { pos ->
                    val lr = layoutResult ?: return@detectTapGestures
                    val offset = lr.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { ann ->
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("link", ann.item))
                            Toast.makeText(context, "لینک کپی شد", Toast.LENGTH_SHORT).show()
                        }
                },
                onTap = { pos ->
                    val lr = layoutResult ?: return@detectTapGestures
                    val offset = lr.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { ann ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item)))
                            } catch (_: Exception) {
                                Toast.makeText(context, "نتوانست لینک را باز کند", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            )
        },
        style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 22.sp),
        onTextLayout = { layoutResult = it },
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { ann ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item)))
                    } catch (_: Exception) {
                        Toast.makeText(context, "نتوانست لینک را باز کند", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    )
}

@Composable
fun AnnouncementDialog(announcement: Announcement, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val urls = remember(announcement.body) { AnnouncementRepository.extractUrls(announcement.body) }

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

                LinkifiedBodyText(
                    text = announcement.body,
                    modifier = Modifier.fillMaxWidth()
                )

                if (urls.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    urls.distinct().forEach { raw ->
                        val url = AnnouncementRepository.normalizeUrl(raw)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "نتوانست لینک را باز کند", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("باز کردن لینک", fontSize = 12.sp, color = AppPalette.accent)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("link", url))
                                    Toast.makeText(context, "لینک کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("کپی لینک", fontSize = 12.sp, color = AppPalette.textSecondary)
                            }
                        }
                    }
                }

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
