package com.quietanswerer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietanswerer.R

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }

    val titles = listOf(
        context.getString(R.string.onboarding_step1_title),
        context.getString(R.string.onboarding_step2_title),
        context.getString(R.string.onboarding_step3_title)
    )
    val texts = listOf(
        context.getString(R.string.onboarding_step1_text),
        context.getString(R.string.onboarding_step2_text),
        context.getString(R.string.onboarding_step3_text)
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                context.getString(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))
            WidgetPreview()
            Spacer(Modifier.height(32.dp))
            Text(titles[page], style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                texts[page],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text(context.getString(R.string.skip))
                }
                Button(
                    onClick = {
                        if (page == 2) onDone() else page++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (page == 2) context.getString(R.string.done) else context.getString(R.string.next))
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                LocalContext.current.getString(R.string.on),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                LocalContext.current.getString(R.string.until_time, "14:30"),
                color = Color(0xFFFFF3F2),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            LocalContext.current.getString(R.string.status_default_reply),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33FFFFFF))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.NotificationsOff, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                LocalContext.current.getString(R.string.turn_off),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}