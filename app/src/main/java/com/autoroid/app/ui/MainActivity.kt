package com.autoroid.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.autoroid.app.ui.screens.HomeScreen
import com.autoroid.app.ui.theme.AutoroidTheme
import rikka.shizuku.Shizuku

import android.os.Build
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var openUpdateOnLaunch by mutableStateOf(false)

    private val requestPhonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.log("READ_PHONE_STATE permission granted.")
                viewModel.refreshAll()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.log("POST_NOTIFICATIONS permission granted.")
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.log("Shizuku binder received from service.")
        checkAndRefreshShizuku()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        viewModel.log("Shizuku binder died.")
        viewModel.refreshAll()
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    viewModel.log("Shizuku permission GRANTED.")
                    viewModel.refreshAll()
                } else {
                    viewModel.log("Shizuku permission was DENIED.")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openUpdateOnLaunch = intent?.getBooleanExtra(com.autoroid.app.feature.update.UpdateManager.EXTRA_OPEN_UPDATE, false) ?: false

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPhonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AutoroidTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onRequestShizuku = { requestShizukuPermission() },
                    initialShowUpdateDialog = openUpdateOnLaunch
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(com.autoroid.app.feature.update.UpdateManager.EXTRA_OPEN_UPDATE, false)) {
            openUpdateOnLaunch = true
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRefreshShizuku()
    }

    private fun checkAndRefreshShizuku() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    viewModel.refreshAll()
                }
            } else {
                viewModel.refreshAll()
            }
        } catch (e: Throwable) {
            // Ignore if binder not ready yet
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                viewModel.log("Shizuku service is not running or binder is unavailable.")
                val pm = packageManager
                val launchIntent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (launchIntent != null) {
                    viewModel.log("Opening Shizuku Manager...")
                    startActivity(launchIntent)
                } else {
                    viewModel.log("Shizuku Manager is not installed. Visit https://shizuku.rikka.app")
                }
                return
            }

            if (Shizuku.isPreV11()) {
                viewModel.log("Shizuku pre-v11 is not supported.")
                return
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                viewModel.log("Shizuku permission is already granted.")
                viewModel.refreshAll()
                return
            }

            viewModel.log("Requesting Shizuku authorization dialog...")
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        } catch (e: Throwable) {
            viewModel.log("Error requesting Shizuku: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}

