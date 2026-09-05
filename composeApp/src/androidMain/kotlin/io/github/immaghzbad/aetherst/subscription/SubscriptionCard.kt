package io.github.immaghzbad.aetherst.subscription

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubscriptionCard(viewModel: SubscriptionViewModel = viewModel()) {
    val info by viewModel.subscriptionInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.activationMessage.collectAsState()

    var showActivateDialog by remember { mutableStateOf(false) }
    var showExtendDialog by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(AppPalette.accent.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            if (isLoading && info == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppPalette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                val currentInfo = info
                val remainingMillis = (currentInfo?.expiresAtMillis ?: 0L) - now
                val isActive = currentInfo?.type == "paid" && remainingMillis > 0

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اشتراک",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isActive) AppPalette.statusConnected else AppPalette.statusError,
                                        RoundedCornerShape(50)
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isActive) "فعال" else "منقضی شده",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) AppPalette.statusConnected else AppPalette.statusError
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isActive) {
                        val days = remainingMillis / (1000 * 60 * 60 * 24)
                        val hours = (remainingMillis / (1000 * 60 * 60)) % 24
                        val minutes = (remainingMillis / (1000 * 60)) % 60
                        val seconds = (remainingMillis / 1000) % 60

                        Text(
                            text = when {
                                days > 0 -> "$days روز، $hours ساعت ${if (days <= 3) "$minutes دقیقه" else ""} باقی مانده"
                                hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
                                else -> "%02d:%02d".format(minutes, seconds)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = if (days > 0) 20.sp else 24.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        val totalDays = 30
                        val progress = (remainingMillis / (1000f * 60 * 60 * 24)) / totalDays
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0.01f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (progress < 0.2f) AppPalette.statusScanning else AppPalette.accent,
                            trackColor = AppPalette.divider
                        )

                        if (days <= 3 && days >= 0) {
                            Spacer(Modifier.height(4.dp))
                            val expiryDate = SimpleDateFormat("dd MMM yyyy", Locale("fa"))
                                .format(Date(currentInfo?.expiresAtMillis ?: 0))
                            Text(
                                text = "تاریخ انقضا: $expiryDate",
                                color = AppPalette.statusScanning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = if (currentInfo?.type?.startsWith("error") == true)
                                "⚠️ خطا در بررسی اشتراک. اتصال خود را بررسی کنید."
                            else if (currentInfo?.type == "pending")
                                "⏳ کد فعال‌سازی در انتظار تایید است..."
                            else
                                "🔑 اشتراک فعالی وجود ندارد. کد فعال‌سازی را وارد کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textSecondary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isActive) {
                                showExtendDialog = true
                            } else {
                                showActivateDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) AppPalette.accent.copy(alpha = 0.8f) else AppPalette.accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isActive) "🔁 تمدید اشتراک" else "🚀 فعال‌سازی",
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.onAccent
                        )
                    }
                }
            }
        }
    }

    if (showActivateDialog) {
        ActivateCodeDialog(
            onDismiss = {
                showActivateDialog = false
            },
            onActivate = { code -> viewModel.activateCode(code, "") },
            onRefresh = { viewModel.refreshStatus() },
            message = message,
            onMessageShown = { viewModel.clearMessage() },
            deviceId = viewModel.deviceId
        )
    }

    if (showExtendDialog) {
        ExtendSubscriptionDialog(
            onDismiss = {
                showExtendDialog = false
            },
            onExtend = { code -> viewModel.extendSubscription(code) },
            onRefresh = { viewModel.refreshStatus() },
            message = message,
            onMessageShown = { viewModel.clearMessage() },
            deviceId = viewModel.deviceId,
            currentInfo = info
        )
    }
}

@Composable
private fun ActivateCodeDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit,
    onRefresh: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    deviceId: String
) {
    var code by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(message) {
        if (message != null && message.isNotBlank()) {
            when {
                message.contains("فعال شد", ignoreCase = true) ||
                message.contains("موفق", ignoreCase = true) ||
                message.contains("success", ignoreCase = true) ||
                message.contains("به‌روز", ignoreCase = true) -> {
                    onRefresh()
                    delay(1500)
                    isActivating = false
                    onDismiss()
                    onMessageShown()
                    Toast.makeText(context, "✅ اشتراک فعال شد!", Toast.LENGTH_LONG).show()
                }
                message.contains("نامعتبر", ignoreCase = true) ||
                message.contains("invalid", ignoreCase = true) ||
                message.contains("خطا", ignoreCase = true) ||
                message.contains("error", ignoreCase = true) -> {
                    delay(2500)
                    isActivating = false
                    onMessageShown()
                }
                message.contains("منتظر", ignoreCase = true) ||
                message.contains("pending", ignoreCase = true) ||
                message.contains("ارسال", ignoreCase = true) ||
                message.contains("تایید", ignoreCase = true) -> {
                    isActivating = false
                }
                else -> {
                    delay(3000)
                    isActivating = false
                    onMessageShown()
                }
            }
        }
    }

    Dialog(onDismissRequest = {
        if (!isActivating) {
            onDismiss()
            onMessageShown()
        }
    }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "فعال‌سازی اشتراک",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "کد فعال‌سازی را وارد کنید",
                    color = AppPalette.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "📱 شناسه دستگاه (برای کپی کلیک کنید، به مدیر ارسال کنید):",
                    color = AppPalette.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppPalette.divider)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(deviceId))
                            Toast.makeText(context, "✅ شناسه دستگاه کپی شد!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deviceId,
                        color = AppPalette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "کپی",
                        tint = AppPalette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                    },
                    label = { Text("کد فعال‌سازی") },
                    placeholder = { Text("کد ۱۶ رقمی را وارد کنید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActivating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPalette.accent,
                        unfocusedBorderColor = AppPalette.divider
                    )
                )

                if (message != null && message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.contains("فعال شد", ignoreCase = true) ||
                                message.contains("موفق", ignoreCase = true) ||
                                message.contains("success", ignoreCase = true) ||
                                message.contains("به‌روز", ignoreCase = true))
                                AppPalette.statusConnected.copy(alpha = 0.15f)
                            else if (message.contains("منتظر", ignoreCase = true) ||
                                message.contains("pending", ignoreCase = true) ||
                                message.contains("ارسال", ignoreCase = true))
                                AppPalette.statusScanning.copy(alpha = 0.15f)
                            else
                                AppPalette.statusError.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                color = if (message.contains("فعال شد", ignoreCase = true) ||
                                    message.contains("موفق", ignoreCase = true) ||
                                    message.contains("success", ignoreCase = true) ||
                                    message.contains("به‌روز", ignoreCase = true))
                                    AppPalette.statusConnected
                                else if (message.contains("منتظر", ignoreCase = true) ||
                                    message.contains("pending", ignoreCase = true) ||
                                    message.contains("ارسال", ignoreCase = true))
                                    AppPalette.statusScanning
                                else
                                    AppPalette.statusError,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    onMessageShown()
                                    if (message.contains("فعال شد", ignoreCase = true) ||
                                        message.contains("موفق", ignoreCase = true) ||
                                        message.contains("success", ignoreCase = true)) {
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = AppPalette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            isActivating = true
                            onActivate(code)
                        }
                    },
                    enabled = code.isNotBlank() && !isActivating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)
                ) {
                    if (isActivating) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(
                                color = AppPalette.onAccent,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("در حال بررسی...", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                        }
                    } else {
                        Text("✅ بررسی کد", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (!isActivating) {
                            onRefresh()
                            Toast.makeText(context, "🔄 در حال به‌روزرسانی وضعیت...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActivating
                ) {
                    Text(
                        "🔄 شناسه دستگاه را ارسال کردم، وضعیت را به‌روز کن",
                        color = AppPalette.accent,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = {
                        if (!isActivating) {
                            onDismiss()
                            onMessageShown()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActivating
                ) {
                    Text("انصراف", color = AppPalette.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ExtendSubscriptionDialog(
    onDismiss: () -> Unit,
    onExtend: (String) -> Unit,
    onRefresh: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    deviceId: String,
    currentInfo: SubscriptionInfo?
) {
    var code by remember { mutableStateOf("") }
    var isExtending by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val remainingDays = if (currentInfo != null && currentInfo.isActive) {
        val remaining = currentInfo.expiresAtMillis - System.currentTimeMillis()
        (remaining / (1000 * 60 * 60 * 24)).toInt()
    } else 0

    LaunchedEffect(message) {
        if (message != null && message.isNotBlank()) {
            when {
                message.contains("تمدید", ignoreCase = true) ||
                message.contains("موفق", ignoreCase = true) ||
                message.contains("success", ignoreCase = true) -> {
                    delay(1500)
                    isExtending = false
                    onDismiss()
                    onMessageShown()
                    Toast.makeText(context, "✅ اشتراک تمدید شد!", Toast.LENGTH_LONG).show()
                }
                message.contains("نامعتبر", ignoreCase = true) ||
                message.contains("invalid", ignoreCase = true) ||
                message.contains("خطا", ignoreCase = true) -> {
                    delay(2500)
                    isExtending = false
                    onMessageShown()
                }
                message.contains("منتظر", ignoreCase = true) ||
                message.contains("pending", ignoreCase = true) ||
                message.contains("ارسال", ignoreCase = true) ||
                message.contains("تایید", ignoreCase = true) -> {
                    isExtending = false
                }
                else -> {
                    delay(3000)
                    isExtending = false
                    onMessageShown()
                }
            }
        }
    }

    Dialog(onDismissRequest = {
        if (!isExtending) {
            onDismiss()
            onMessageShown()
        }
    }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🔄 تمدید اشتراک",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    if (remainingDays > 0) "زمان باقیمانده: $remainingDays روز" else "اشتراک در حال انقضا است",
                    color = if (remainingDays > 0) AppPalette.statusConnected else AppPalette.statusScanning,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "کد تمدید را وارد کنید",
                    color = AppPalette.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "📱 شناسه دستگاه:",
                    color = AppPalette.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppPalette.divider)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(deviceId))
                            Toast.makeText(context, "✅ شناسه دستگاه کپی شد!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deviceId,
                        color = AppPalette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "کپی",
                        tint = AppPalette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                    },
                    label = { Text("کد تمدید") },
                    placeholder = { Text("کد تمدید را وارد کنید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExtending,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPalette.accent,
                        unfocusedBorderColor = AppPalette.divider
                    )
                )

                if (message != null && message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.contains("موفق", ignoreCase = true) ||
                                message.contains("success", ignoreCase = true))
                                AppPalette.statusConnected.copy(alpha = 0.15f)
                            else
                                AppPalette.statusError.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (message.contains("موفق", ignoreCase = true) ||
                                message.contains("success", ignoreCase = true))
                                AppPalette.statusConnected
                            else
                                AppPalette.statusError,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            isExtending = true
                            onExtend(code)
                        }
                    },
                    enabled = code.isNotBlank() && !isExtending,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)
                ) {
                    if (isExtending) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(
                                color = AppPalette.onAccent,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("در حال تمدید...", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                        }
                    } else {
                        Text("✅ تمدید اشتراک", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (!isExtending) {
                            onRefresh()
                            Toast.makeText(context, "🔄 در حال به‌روزرسانی وضعیت...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExtending
                ) {
                    Text(
                        "🔄 وضعیت را به‌روز کن",
                        color = AppPalette.accent,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = {
                        if (!isExtending) {
                            onDismiss()
                            onMessageShown()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExtending
                ) {
                    Text("انصراف", color = AppPalette.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
