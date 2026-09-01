package io.github.immaghzbad.aetherst.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import kotlinx.coroutines.delay

@Composable
fun SubscriptionCard(viewModel: SubscriptionViewModel = viewModel()) {
    val info by viewModel.subscriptionInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.activationMessage.collectAsState()

    var showActivateDialog by remember { mutableStateOf(false) }
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
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppPalette.accent, modifier = Modifier.size(24.dp))
                }
            } else {
                val currentInfo = info
                val remainingMillis = (currentInfo?.expiresAtMillis ?: 0L) - now
                val isActive = remainingMillis > 0

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentInfo?.type) {
                                "trial" -> "FREE TRIAL"
                                "paid" -> "SUBSCRIPTION"
                                else -> "SUBSCRIPTION"
                            },
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
                                text = if (isActive) "ACTIVE" else "EXPIRED",
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
                            text = if (days > 0) "$days days, $hours hours left" else "%02d:%02d:%02d".format(hours, minutes, seconds),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 22.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        val totalDuration = 30f
                        val progress = (remainingMillis / (1000f * 60 * 60 * 24)) / totalDuration
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0.02f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = AppPalette.accent,
                            trackColor = AppPalette.divider
                        )
                    } else {
                        Text(
                            text = "Your subscription has expired.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textSecondary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showActivateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isActive) "Enter Activation Code" else "Activate Now",
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
            onDismiss = { showActivateDialog = false },
            onActivate = { code, telegramId ->
                viewModel.activateCode(code, telegramId)
            },
            message = message,
            onMessageShown = { viewModel.clearMessage() },
            onSuccess = { showActivateDialog = false }
        )
    }
}

@Composable
private fun ActivateCodeDialog(
    onDismiss: () -> Unit,
    onActivate: (String, String) -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    onSuccess: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var telegramId by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message == "Activated successfully!") {
            delay(800)
            onSuccess()
            onMessageShown()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Activate Subscription",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Activation Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = telegramId,
                    onValueChange = { telegramId = it },
                    label = { Text("Telegram ID (e.g. @username)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null && message != "Activated successfully!") {
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = AppPalette.statusError, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onActivate(code, telegramId) },
                    enabled = code.isNotBlank() && telegramId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.accent)
                ) {
                    Text("ACTIVATE", fontWeight = FontWeight.Bold, color = AppPalette.onAccent)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = AppPalette.textSecondary)
                }
            }
        }
    }
}
