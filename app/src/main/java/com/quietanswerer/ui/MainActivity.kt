package com.quietanswerer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietanswerer.App
import com.quietanswerer.core.Actions
import com.quietanswerer.core.Prefs
import com.quietanswerer.ui.screens.HomeScreen
import com.quietanswerer.ui.screens.OnboardingScreen
import com.quietanswerer.ui.screens.RepliesScreen
import com.quietanswerer.ui.screens.SettingsScreen
import com.quietanswerer.ui.theme.ApplyStatusBarStyle
import com.quietanswerer.ui.theme.QuietAnswererTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTabExtra(intent)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            QuietAnswererTheme(mode = state.themeMode) {
                ApplyStatusBarStyle(state.themeMode)
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyTabExtra(intent)
    }

    private fun applyTabExtra(intent: Intent?) {
        val tab = intent?.getIntExtra(Actions.EXTRA_TAB, -1) ?: -1
        if (tab >= 0) viewModel.requestTab(tab)
    }
}

@androidx.compose.runtime.Composable
fun AppRoot(vm: AppViewModel) {
    val context = LocalContext.current.applicationContext as App
    var onboardingDone by rememberSaveable { mutableStateOf(Prefs.onboardingDone(context)) }
    var screen by rememberSaveable { mutableIntStateOf(0) } // 0 home, 1 settings, 2 replies
    var repliesCallId by rememberSaveable { mutableLongStateOf(-1L) }
    var repliesNumber by rememberSaveable { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // Внешний запрос на вкладку (уведомление/виджет) возвращает на главный экран.
    val tabRequest by vm.tabRequest.collectAsStateWithLifecycle()
    LaunchedEffect(tabRequest) {
        if (tabRequest >= 0) screen = 0
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_CALL_LOG)
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_CONTACTS)
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= 28 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    if (!onboardingDone) {
        OnboardingScreen(
            onDone = {
                Prefs.setOnboardingDone(context, true)
                onboardingDone = true
            }
        )
        return
    }

    when (screen) {
        1 -> SettingsScreen(
            vm = vm,
            onBack = { screen = 0 }
        )
        2 -> RepliesScreen(
            vm = vm,
            callId = repliesCallId,
            number = repliesNumber,
            onBack = { screen = 0 }
        )
        else -> HomeScreen(
            vm = vm,
            onOpenSettings = { screen = 1 },
            onOpenReplies = { callId, number ->
                repliesCallId = callId
                repliesNumber = number
                screen = 2
            }
        )
    }
}