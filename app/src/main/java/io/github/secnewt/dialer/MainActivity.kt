package io.github.secnewt.dialer

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.dialer.screening.SpamSettingsStore
import io.github.secnewt.dialer.ui.BlockListScreen
import io.github.secnewt.dialer.ui.SpamProtectionScreen
import io.github.secnewt.dialer.ui.SpamSettingsScreen
import io.github.secnewt.dialer.ui.theme.DialerTheme

private enum class Screen { HOME, SETTINGS, BLOCK_LIST }

class MainActivity : ComponentActivity() {

    private val store by lazy { SpamSettingsStore(this) }

    private var screeningRoleHeld by mutableStateOf(false)
    private var recentCalls by mutableStateOf(emptyList<ScreenedCall>())
    private var settings by mutableStateOf(SpamSettings())
    private var blockRules by mutableStateOf(emptyList<BlockRule>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val roleManager = getSystemService(RoleManager::class.java)
        setContent {
            DialerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                val requestRole = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { refresh() }

                BackHandler(enabled = screen != Screen.HOME) {
                    screen = if (screen == Screen.BLOCK_LIST) Screen.SETTINGS else Screen.HOME
                }

                when (screen) {
                    Screen.HOME -> SpamProtectionScreen(
                        roleHeld = screeningRoleHeld,
                        recentCalls = recentCalls,
                        observeOnly = settings.observeOnly,
                        blockRules = blockRules,
                        onEnable = {
                            requestRole.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            )
                        },
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onBlock = { addRule(BlockRule.Number(it)) },
                        onUnblock = ::removeNumber,
                    )
                    Screen.SETTINGS -> SpamSettingsScreen(
                        settings = settings,
                        blockListSize = blockRules.size,
                        onSettingsChange = ::updateSettings,
                        onOpenBlockList = { screen = Screen.BLOCK_LIST },
                        onBack = { screen = Screen.HOME },
                    )
                    Screen.BLOCK_LIST -> BlockListScreen(
                        rules = blockRules,
                        onAdd = ::addRule,
                        onRemove = { rule -> saveRules(blockRules - rule) },
                        onBack = { screen = Screen.SETTINGS },
                    )
                }
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
        settings = store.settings()
        blockRules = store.blockRules()
    }

    private fun updateSettings(newSettings: SpamSettings) {
        settings = newSettings
        store.saveSettings(newSettings)
    }

    private fun addRule(rule: BlockRule) {
        if (rule !in blockRules) saveRules(blockRules + rule)
    }

    /** Removes exact-number rules for this caller, whatever formatting they were saved with. */
    private fun removeNumber(number: String) {
        val target = PhoneNumbers.normalize(number)
        saveRules(blockRules.filterNot { it is BlockRule.Number && PhoneNumbers.normalize(it.number) == target })
    }

    private fun saveRules(rules: List<BlockRule>) {
        blockRules = rules
        store.saveBlockRules(rules)
    }
}
