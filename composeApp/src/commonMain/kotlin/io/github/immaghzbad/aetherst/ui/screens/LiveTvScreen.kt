package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
expect fun PlatformWebView(url: String, modifier: Modifier = Modifier)

@Composable
fun LiveTvScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("پخش زنده ایران اینترنشنال")
        }
        PlatformWebView(
            url = "https://www.iranintl.com/live",
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}
