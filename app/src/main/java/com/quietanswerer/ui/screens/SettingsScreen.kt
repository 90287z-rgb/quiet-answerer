package com.quietanswerer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietanswerer.App
import com.quietanswerer.R
import com.quietanswerer.data.db.IncomingCall
import com.quietanswerer.data.db.Reply
import com.quietanswerer.ui.AppViewModel
import com.quietanswerer.ui.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TIME_FMT = SimpleDateFormat("HH:mm", Locale.getDefault())
private val DATE_FMT = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var openFrequency by rememberSaveable { mutableStateOf(false) }
    var openTheme by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canMute) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(context.getString(R.string.dnd_required_title), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.padding(4.dp))
                            Text(context.getString(R.string.dnd_required_desc))
                            Spacer(Modifier.padding(4.dp))
                            Button(onClick = {
                                val cm = context.applicationContext as App
                                com.quietanswerer.audio.openDndSettings(cm)
                            }) {
                                Text(context.getString(R.string.dnd_open_settings))
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle(context.getString(R.string.settings_end_calls))
                Card {
                    SettingSwitchRow(
                        title = context.getString(R.string.settings_end_calls),
                        subtitle = context.getString(R.string.settings_end_calls_desc),
                        checked = state.endCalls,
                        onChecked = vm::setEndCalls
                    )
                }
            }

            item {
                SectionTitle(context.getString(R.string.settings_sms))
                Card {
                    SettingSwitchRow(
                        title = context.getString(R.string.settings_friends_only),
                        subtitle = context.getString(R.string.settings_friends_only_desc),
                        checked = state.contactRequired,
                        onChecked = vm::setContactRequired
                    )
                    androidx.compose.material3.HorizontalDivider()
                    val freqLabel = frequencyLabel(context, state.frequencyMin)
                    Row(
                        Modifier.fillMaxWidth().clickable { openFrequency = true }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            context.getString(R.string.settings_frequency) + " — $freqLabel",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SectionTitle(context.getString(R.string.settings_mute))
                Card {
                    SettingSwitchRow(context.getString(R.string.settings_mute_music), null, state.muteMusic, vm::setMuteMusic)
                    androidx.compose.material3.HorizontalDivider()
                    SettingSwitchRow(context.getString(R.string.settings_mute_notifications), null, state.muteNotifications, vm::setMuteNotifications)
                    androidx.compose.material3.HorizontalDivider()
                    SettingSwitchRow(context.getString(R.string.settings_mute_system), null, state.muteSystem, vm::setMuteSystem)
                    androidx.compose.material3.HorizontalDivider()
                    SettingSwitchRow(
                        context.getString(R.string.settings_mute_alarm),
                        context.getString(R.string.settings_mute_alarm_desc),
                        state.muteAlarm,
                        vm::setMuteAlarm
                    )
                }
            }

            item {
                SectionTitle(context.getString(R.string.settings_appearance))
                Card {
                    Row(
                        Modifier.fillMaxWidth().clickable { openTheme = true }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            context.getString(R.string.settings_theme) + " — " + themeLabel(context, state.themeMode),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (openFrequency) {
        FrequencyDialog(
            current = state.frequencyMin,
            onPick = { vm.setFrequency(it); openFrequency = false },
            onDismiss = { openFrequency = false }
        )
    }

    if (openTheme) {
        ThemeDialog(
            current = state.themeMode,
            onPick = { vm.setThemeMode(it); openTheme = false },
            onDismiss = { openTheme = false }
        )
    }
}

private fun themeLabel(context: android.content.Context, mode: Int): String = when (mode) {
    com.quietanswerer.core.Prefs.THEME_LIGHT -> context.getString(R.string.theme_light)
    com.quietanswerer.core.Prefs.THEME_DARK -> context.getString(R.string.theme_dark)
    else -> context.getString(R.string.theme_system)
}

@Composable
private fun ThemeDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(current) }
    val options = listOf(
        com.quietanswerer.core.Prefs.THEME_SYSTEM to context.getString(R.string.theme_system),
        com.quietanswerer.core.Prefs.THEME_LIGHT to context.getString(R.string.theme_light),
        com.quietanswerer.core.Prefs.THEME_DARK to context.getString(R.string.theme_dark)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.settings_theme)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = mode }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == mode, onClick = { selected = mode })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text(context.getString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.cancel)) }
        }
    )
}

private fun frequencyLabel(context: android.content.Context, minutes: Int): String = when (minutes) {
    15 -> context.getString(R.string.frequency_15)
    30 -> context.getString(R.string.frequency_30)
    60 -> context.getString(R.string.frequency_60)
    360 -> context.getString(R.string.frequency_360)
    720 -> context.getString(R.string.frequency_720)
    else -> "$minutes"
}

@Composable
private fun frequencyList(context: android.content.Context): List<Pair<Int, String>> = listOf(
    15 to context.getString(R.string.frequency_15),
    30 to context.getString(R.string.frequency_30),
    60 to context.getString(R.string.frequency_60),
    360 to context.getString(R.string.frequency_360),
    720 to context.getString(R.string.frequency_720)
)

@Composable
private fun FrequencyDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.settings_frequency)) },
        text = {
            Column {
                frequencyList(context).forEach { (min, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = min }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == min, onClick = { selected = min })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text(context.getString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.cancel)) }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(Modifier.padding(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}