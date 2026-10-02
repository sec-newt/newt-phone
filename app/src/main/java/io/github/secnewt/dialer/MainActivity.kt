package io.github.secnewt.dialer

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.ui.SpamProtectionScreen
import io.github.secnewt.dialer.ui.theme.DialerTheme

class MainActivity : ComponentActivity() {

    private var screeningRoleHeld by mutableStateOf(false)
    private var recentCalls by mutableStateOf(emptyList<ScreenedCall>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val roleManager = getSystemService(RoleManager::class.java)
        setContent {
            DialerTheme {
                val requestRole = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { refresh() }
                SpamProtectionScreen(
                    roleHeld = screeningRoleHeld,
                    recentCalls = recentCalls,
                    onEnable = {
                        requestRole.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        screeningRoleHeld = getSystemService(RoleManager::class.java)
            .isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        recentCalls = ScreeningLog(this).read()
    }
}
