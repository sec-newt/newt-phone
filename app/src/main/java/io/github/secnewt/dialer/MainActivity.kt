package io.github.secnewt.dialer

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.ui.theme.DialerTheme

class MainActivity : ComponentActivity() {

    private var screeningRoleHeld by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val roleManager = getSystemService(RoleManager::class.java)
        setContent {
            DialerTheme {
                val requestRole = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { refreshRole() }
                SpamProtectionScreen(
                    roleHeld = screeningRoleHeld,
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
        refreshRole()
    }

    private fun refreshRole() {
        screeningRoleHeld = getSystemService(RoleManager::class.java)
            .isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}

@Composable
private fun SpamProtectionScreen(roleHeld: Boolean, onEnable: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Spam protection",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (roleHeld) "On" else "Off",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (roleHeld) {
                            "This app checks incoming calls before they ring. " +
                                "For now every call is allowed while the rules are built."
                        } else {
                            "Set this app as your Caller ID & spam app so it can check calls before they ring."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!roleHeld) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Turn on spam protection", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
