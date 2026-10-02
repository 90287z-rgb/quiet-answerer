package com.quietanswerer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.quietanswerer.BuildConfig
import com.quietanswerer.R
import com.quietanswerer.data.db.IncomingCall
import com.quietanswerer.data.db.Reply
import com.quietanswerer.ui.AppViewModel
import com.quietanswerer.ui.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TIME_FMT = SimpleDateFormat("HH:mm", Locale.getDefault())
private val DATE_FMT = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenReplies: (callId: Long, number: String) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Вкладки — единый источник состояния во ViewModel: внешние intent'ы (уведомление/виджет)
    // могут переключать вкладку, а ручное переключение должно участвовать в той же потоке,
    // иначе повторный tap по тому же EXTRA_TAB не «сработает» повторно.
    val tabRequest by vm.tabRequest.collectAsStateWithLifecycle()
    val tabIndex = if (tabRequest >= 0) tabRequest else 0
    var showTimerMenu by remember { mutableStateOf(false) }
    var showCustomTime by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLikeIt by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun showError(key: Int) {
        scope.launch { snackbar.showSnackbar(context.getString(key)) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(context.getString(R.string.app_name), fontWeight = FontWeight.Bold)
                            if (state.autoStopAt > 0) {
                                Text(
                                    text = context.getString(R.string.timer_active, TIME_FMT.format(Date(state.autoStopAt))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        if (tabIndex == 1) {
                            IconButton(onClick = { if (state.calls.isNotEmpty()) showClearConfirm = true }) {
                                Icon(Icons.Outlined.Delete, contentDescription = context.getString(R.string.clear_log))
                            }
                        }
                        IconButton(onClick = { showTimerMenu = true }) {
                            Icon(Icons.Outlined.Alarm, contentDescription = context.getString(R.string.timer_title))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = context.getString(R.string.settings))
                        }
                        MoreMenu(
                            onHelp = { showHelp = true },
                            onAbout = { showAbout = true },
                            onLikeIt = { showLikeIt = true }
                        )
                    }
                )
                if (state.sessionOpen && !state.canMute) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.NotificationsOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                context.getString(R.string.dnd_required_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.toggle() },
                containerColor = if (state.sessionOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (state.sessionOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = {
                    Icon(
                        if (state.sessionOpen) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                        null
                    )
                },
                text = {
                    Text(if (state.sessionOpen) context.getString(R.string.turn_off) else context.getString(R.string.turn_on))
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { vm.requestTab(0) }, text = { Text(context.getString(R.string.tab_status)) })
                Tab(selected = tabIndex == 1, onClick = { vm.requestTab(1) }, text = { Text(context.getString(R.string.tab_log)) })
                Tab(selected = tabIndex == 2, onClick = { vm.requestTab(2) }, text = { Text(context.getString(R.string.tab_vip)) })
            }
            // Высота расширенной FAB (56dp) + её нижний отступ Scaffold (16dp): контент не должен
            // залезать под кнопку «Включить/Выключить».
            val fabInset = 72.dp
            when (tabIndex) {
                1 -> LogTab(state, vm, onOpenReplies, fabInset)
                2 -> VipTab(state, vm, fabInset)
                else -> MessageTab(state, vm, ::showError, fabInset)
            }
        }
    }

    if (showTimerMenu) {
        TimerMenu(
            state = state,
            onDismiss = { showTimerMenu = false },
            onPreset = {
                showTimerMenu = false
                vm.setTimerMinutes(it)
            },
            onCustom = {
                showTimerMenu = false
                showCustomTime = true
            },
            onClear = {
                showTimerMenu = false
                vm.clearTimer()
            }
        )
    }

    if (showCustomTime) {
        CustomTimeDialog(
            onDismiss = { showCustomTime = false },
            onPicked = { epoch ->
                showCustomTime = false
                vm.setTimerAt(epoch)
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(context.getString(R.string.clear_log)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    vm.clearLog()
                }) { Text(context.getString(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(context.getString(R.string.cancel)) }
            }
        )
    }

    if (showHelp) {
        InfoDialog(
            title = context.getString(R.string.help),
            text = context.getString(R.string.help_text),
            onDismiss = { showHelp = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showLikeIt) {
        LikeItDialog(onDismiss = { showLikeIt = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenu(onHelp: () -> Unit, onAbout: () -> Unit, onLikeIt: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Info, contentDescription = "menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringRes(R.string.help)) }, onClick = { expanded = false; onHelp() })
            DropdownMenuItem(text = { Text(stringRes(R.string.about)) }, onClick = { expanded = false; onAbout() })
            DropdownMenuItem(text = { Text(stringRes(R.string.like_it)) }, onClick = { expanded = false; onLikeIt() })
        }
    }
}

@androidx.compose.runtime.Composable
private fun stringRes(id: Int): String {
    val c = LocalContext.current
    return c.getString(id)
}

@Composable
fun TimerMenu(
    state: UiState,
    onDismiss: () -> Unit,
    onPreset: (Int) -> Unit,
    onCustom: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        if (state.autoStopAt > 0) {
            DropdownMenuItem(
                text = {
                    Text(context.getString(R.string.timer_active, TIME_FMT.format(Date(state.autoStopAt))))
                },
                onClick = {},
                enabled = false
            )
            HorizontalDivider()
        }
        for (minutes in intArrayOf(5, 15, 30, 45, 60)) {
            DropdownMenuItem(
                text = { Text("$minutes min") },
                onClick = { onPreset(minutes) }
            )
        }
        DropdownMenuItem(
            text = { Text(context.getString(R.string.timer_custom)) },
            onClick = onCustom
        )
        if (state.autoStopAt > 0) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.timer_disable)) },
                onClick = onClear
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimeDialog(onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val context = LocalContext.current
    val now = Calendar.getInstance()
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.timer_custom_hint)) },
        text = {
            androidx.compose.material3.TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, state.hour)
                cal.set(Calendar.MINUTE, state.minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                var t = cal.timeInMillis
                if (t <= System.currentTimeMillis()) t += 24 * 60 * 60 * 1000L
                onPicked(t)
            }) { Text(context.getString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.cancel)) }
        }
    )
}

@Composable
private fun MessageTab(state: UiState, vm: AppViewModel, onError: (Int) -> Unit, fabInset: Dp) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.statuses, key = { it.id }) { status ->
                StatusRow(
                    isActive = status.id == state.currentStatusId,
                    onSelect = { vm.setActiveStatus(status.id) },
                    onDuplicate = { vm.duplicateStatus(status.id) },
                    onRemove = { vm.removeStatus(status.id) },
                    label = status.name,
                    message = status.message
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = fabInset + 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(context.getString(R.string.new_status_placeholder)) },
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        if (!vm.addStatus(text)) {
                            onError(if (text.isBlank()) R.string.status_empty else R.string.status_too_long)
                        }
                        text = ""
                    }
                ) {
                    Icon(Icons.Outlined.Add, null)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StatusRow(
    isActive: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    label: String,
    message: String
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { menu = true }
            ),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isActive, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (message.isNotBlank()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.Check, null, tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringRes(R.string.duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringRes(R.string.remove)) }, onClick = { menu = false; onRemove() })
                }
            }
        }
    }
}

@Composable
private fun LogTab(
    state: UiState,
    vm: AppViewModel,
    onOpenReplies: (Long, String) -> Unit,
    fabInset: Dp
) {
    val context = LocalContext.current
    if (state.calls.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                context.getString(R.string.log_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + fabInset
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.calls, key = { it.id }) { call ->
            CallRow(call, state, onClick = { onOpenReplies(call.id, call.phone_number) })
        }
    }
}

@Composable
private fun CallRow(call: IncomingCall, state: UiState, onClick: () -> Unit) {
    val context = LocalContext.current
    val replies = state.repliesByCall[call.id] ?: emptyList()
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.ExitToApp,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        call.phone_number.ifBlank { context.getString(R.string.contact_unknown) },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        DATE_FMT.format(Date(call.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (replies.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                replies.forEach { reply ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, tint) = when (reply.status) {
                            Reply.STATUS_SENT -> Icons.Outlined.Done to Color(0xFF2E7D32)
                            Reply.STATUS_FAIL -> Icons.Outlined.Error to MaterialTheme.colorScheme.error
                            else -> Icons.Outlined.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            reply.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusLabel = when (reply.status) {
                            Reply.STATUS_SENT -> R.string.reply_status_sent
                            Reply.STATUS_FAIL -> R.string.reply_status_fail
                            else -> R.string.reply_status_pending
                        }
                        Text(
                            context.getString(statusLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun VipTab(state: UiState, vm: AppViewModel, fabInset: Dp) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) vm.onContactPicked(uri)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = vm::enableAllVips) { Text(context.getString(R.string.enable_all)) }
            TextButton(onClick = vm::disableAllVips) { Text(context.getString(R.string.disable_all)) }
            Spacer(Modifier.weight(1f))
            if (selected.isNotEmpty()) {
                IconButton(onClick = { vm.deleteVips(selected.toList()); selected = emptySet() }) {
                    Icon(Icons.Outlined.Delete, context.getString(R.string.delete_selected))
                }
            }
            IconButton(onClick = { picker.launch(null) }) {
                Icon(Icons.Outlined.PersonAdd, context.getString(R.string.add_contact))
            }
        }
        if (state.vips.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(context.getString(R.string.vip_empty), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + fabInset
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.vips, key = { it.vip.id }) { item ->
                val isSelected = item.vip.id in selected
                Card(
                    Modifier.fillMaxWidth().combinedClickable(
                        onClick = {
                            if (selected.isNotEmpty()) {
                                selected = if (isSelected) selected - item.vip.id else selected + item.vip.id
                            } else {
                                vm.setVipActive(item.vip, !item.vip.is_active)
                            }
                        },
                        onLongClick = {
                            selected = if (isSelected) selected - item.vip.id else selected + item.vip.id
                        }
                    ),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected.isNotEmpty()) {
                            Checkbox(checked = isSelected, onCheckedChange = {
                                selected = if (isSelected) selected - item.vip.id else selected + item.vip.id
                            })
                        } else {
                            Checkbox(checked = item.vip.is_active, onCheckedChange = { vm.setVipActive(item.vip, it) })
                        }
                        VipAvatar(name = item.name, thumb = item.thumbnailUri)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            item.name,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.alpha(if (item.vip.is_active) 1f else 0.45f)
                        )
                        Spacer(Modifier.weight(1f))
                        if (item.vip.is_active) {
                            Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VipAvatar(name: String, thumb: String?) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (thumb != null && thumb.isNotBlank()) {
            AsyncImage(
                model = thumb,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringRes(R.string.done)) } }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.about)) },
        text = {
            Column {
                Text(context.getString(R.string.about_text))
                Spacer(Modifier.height(8.dp))
                Text(context.getString(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium)
                Text(context.getString(R.string.author), style = MaterialTheme.typography.labelMedium)
                Text(
                    context.getString(R.string.about_license) + ": CC BY 3.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = {
                rateApp(context)
            }) { Text(context.getString(R.string.rate_it)) }
        }
    )
}

@Composable
private fun LikeItDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.like_it)) },
        text = { Text(context.getString(R.string.share_it)) },
        confirmButton = {
            TextButton(onClick = { rateApp(context); onDismiss() }) {
                Icon(Icons.Outlined.Star, null)
                Spacer(Modifier.width(4.dp))
                Text(context.getString(R.string.rate_it))
            }
        },
        dismissButton = {
            TextButton(onClick = { shareApp(context); onDismiss() }) {
                Icon(Icons.Outlined.Share, null)
                Spacer(Modifier.width(4.dp))
                Text(context.getString(R.string.share_it))
            }
        }
    )
}

private fun rateApp(context: android.content.Context) {
    val uri = Uri.parse("market://details?id=${context.packageName}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e2: Exception) {
        }
    }
}

private fun shareApp(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.app_name))
    }
    try {
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
    }
}