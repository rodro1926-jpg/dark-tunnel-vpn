package com.example

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.model.VpnStatus
import com.example.service.VpnController
import com.example.ui.MainApp
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val config = viewModel.activeConfig.value ?: viewModel.configs.value.firstOrNull()
            if (config != null) {
                VpnController.startVpn(this, config)
            }
        } else {
            Toast.makeText(this, "VPN permission denied by user", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyApplicationTheme {
                MainApp(
                    viewModel = viewModel
                )
            }
        }
    }

    fun requestVpnAndConnect() {
        val currentStatus = viewModel.vpnStatus.value
        if (currentStatus == VpnStatus.DISCONNECTED || currentStatus == VpnStatus.ERROR) {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnPermissionLauncher.launch(vpnIntent)
            } else {
                val config = viewModel.activeConfig.value ?: viewModel.configs.value.firstOrNull()
                if (config != null) {
                    VpnController.startVpn(this, config)
                }
            }
        } else {
            VpnController.stopVpn(this)
        }
    }
}
