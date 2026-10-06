package io.github.secnewt.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.announce.Announcement
import io.github.secnewt.dialer.announce.CallAnnouncer
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.contacts.ContactsRepository
import io.github.secnewt.dialer.contacts.DndCalls
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.dialer.screening.SpamSettingsStore
import io.github.secnewt.dialer.ui.BlockListScreen
import io.github.secnewt.dialer.ui.ContactDetailScreen
import io.github.secnewt.dialer.ui.ContactsScreen
import io.github.secnewt.dialer.ui.DialerTabBar
import io.github.secnewt.dialer.ui.FavoritesScreen
import io.github.secnewt.dialer.ui.SpamProtectionScreen
import io.github.secnewt.dialer.ui.SpamSettingsScreen
import io.github.secnewt.dialer.ui.Tab
import io.github.secnewt.dialer.ui.theme.DialerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** TABS shows Calls, Favorites or Contacts with the bottom bar; the others are full pages. */
private enum class Screen { TABS, SETTINGS, BLOCK_LIST, CONTACT }

private val ANNOUNCE_PERMISSIONS = arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS,
)

class MainActivity : ComponentActivity() {

    private val store by lazy { SpamSettingsStore(this) }
    private val contactsRepo by lazy { ContactsRepository(this) }

    private var screeningRoleHeld by mutableStateOf(false)
    private var recentCalls by mutableStateOf(emptyList<ScreenedCall>())
    private var settings by mutableStateOf(SpamSettings())
    private var blockRules by mutableStateOf(emptyList<BlockRule>())
    private var announce by mutableStateOf(AnnounceSettings())
    private var announceMessage by mutableStateOf<String?>(null)

    private var contactsAccess by mutableStateOf(false)
    private var contacts by mutableStateOf(emptyList<Contact>())
    private var dndCalls by mutableStateOf(DndCalls.UNKNOWN)
    private var contactsMessage by mutableStateOf<String?>(null)

    /** The mode waiting for the person to grant permissions. */
    private var pendingAnnounceMode: AnnounceMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val roleManager = getSystemService(RoleManager::class.java)
        setContent {
            DialerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.TABS) }
                var tab by rememberSaveable { mutableStateOf(Tab.CALLS) }
                var openContactId by rememberSaveable { mutableStateOf<Long?>(null) }
                var contactQuery by rememberSaveable { mutableStateOf("") }
                val requestRole = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { refresh() }
                val requestAnnouncePermissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val mode = pendingAnnounceMode
                    pendingAnnounceMode = null
                    if (mode != null && results.values.all { it }) {
                        updateAnnounce(announce.copy(mode = mode))
                        announceMessage = null
                    } else {
                        announceMessage = "Announcing needs Phone, Call log and Contacts access. " +
                            "Nothing was changed."
                    }
                }
                val requestContactsPermissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    if (results.values.all { it }) {
                        contactsMessage = null
                        refreshContacts()
                    } else {
                        contactsMessage = "Contacts access wasn't allowed. If no question appeared, turn on " +
                            "Contacts in Settings, Apps, ${getString(R.string.app_name)}, Permissions."
                    }
                }

                BackHandler(enabled = screen != Screen.TABS || tab != Tab.CALLS) {
                    when (screen) {
                        Screen.TABS -> tab = Tab.CALLS
                        Screen.BLOCK_LIST -> screen = Screen.SETTINGS
                        else -> screen = Screen.TABS
                    }
                }

                val openSettings = { screen = Screen.SETTINGS }
                val openContact = { contact: Contact ->
                    openContactId = contact.id
                    screen = Screen.CONTACT
                }
                val allowContacts = { requestContactsPermissions.launch(ContactsRepository.PERMISSIONS) }

                when (screen) {
                    Screen.TABS -> Scaffold(
                        bottomBar = { DialerTabBar(selected = tab, onSelect = { tab = it }) },
                        contentWindowInsets = WindowInsets(0),
                    ) { padding ->
                        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                            when (tab) {
                                Tab.CALLS -> SpamProtectionScreen(
                                    roleHeld = screeningRoleHeld,
                                    recentCalls = recentCalls,
                                    observeOnly = settings.observeOnly,
                                    blockRules = blockRules,
                                    announceMode = announce.mode,
                                    onEnable = {
                                        requestRole.launch(
                                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                        )
                                    },
                                    onOpenSettings = openSettings,
                                    onBlock = { addRule(BlockRule.Number(it)) },
                                    onUnblock = ::removeNumber,
                                )
                                Tab.FAVORITES -> FavoritesScreen(
                                    contacts = contacts,
                                    hasAccess = contactsAccess,
                                    dnd = dndCalls,
                                    onOpenSettings = openSettings,
                                    onAllowAccess = allowContacts,
                                    onOpenContact = openContact,
                                    onCall = ::call,
                                    onOpenDndSettings = ::openDndSettings,
                                    message = contactsMessage,
                                )
                                Tab.CONTACTS -> ContactsScreen(
                                    contacts = contacts,
                                    hasAccess = contactsAccess,
                                    query = contactQuery,
                                    onQueryChange = { contactQuery = it },
                                    onOpenSettings = openSettings,
                                    onAllowAccess = allowContacts,
                                    onOpenContact = openContact,
                                    onToggleStar = ::toggleStar,
                                    message = contactsMessage,
                                )
                            }
                        }
                    }
                    Screen.SETTINGS -> SpamSettingsScreen(
                        settings = settings,
                        blockListSize = blockRules.size,
                        onSettingsChange = ::updateSettings,
                        onOpenBlockList = { screen = Screen.BLOCK_LIST },
                        onBack = { screen = Screen.TABS },
                        announce = announce,
                        announceMessage = announceMessage,
                        onAnnounceModeChange = { mode ->
                            announceMessage = null
                            if (mode == AnnounceMode.OFF || hasAnnouncePermissions()) {
                                updateAnnounce(announce.copy(mode = mode))
                            } else {
                                pendingAnnounceMode = mode
                                requestAnnouncePermissions.launch(ANNOUNCE_PERMISSIONS)
                            }
                        },
                        onQuietDuringDndChange = { updateAnnounce(announce.copy(quietDuringDnd = it)) },
                        onTestAnnouncement = ::testAnnouncement,
                    )
                    Screen.BLOCK_LIST -> BlockListScreen(
                        rules = blockRules,
                        onAdd = ::addRule,
                        onRemove = { rule -> saveRules(blockRules - rule) },
                        onBack = { screen = Screen.SETTINGS },
                    )
                    Screen.CONTACT -> {
                        val contact = contacts.firstOrNull { it.id == openContactId }
                        if (contact == null) {
                            // Deleted or no longer visible: go back to the list.
                            LaunchedEffect(Unit) { screen = Screen.TABS }
                        } else {
                            ContactDetailScreen(
                                contact = contact,
                                dnd = dndCalls,
                                onBack = { screen = Screen.TABS },
                                onToggleStar = { toggleStar(contact) },
                                onCall = ::call,
                            )
                        }
                    }
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
        announce = store.announceSettings()
        refreshContacts()
        if (announce.mode != AnnounceMode.OFF && !hasAnnouncePermissions()) {
            announceMessage = "Announcing is on, but Phone, Call log or Contacts access was turned off. " +
                "Choose Announce again to allow it."
        }
    }

    private fun refreshContacts() {
        dndCalls = contactsRepo.dndCalls()
        contactsAccess = contactsRepo.hasAccess()
        if (!contactsAccess) {
            contacts = emptyList()
            return
        }
        lifecycleScope.launch {
            contacts = withContext(Dispatchers.IO) {
                try {
                    contactsRepo.load()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    private fun toggleStar(contact: Contact) {
        val starred = !contact.starred
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { contactsRepo.setStarred(contact.id, starred) }
            if (saved) {
                contacts = ContactList.withStar(contacts, contact.id, starred)
                contactsMessage = null
            } else {
                contactsMessage = "Couldn't change favorites for ${contact.name}. If Contact Scopes is on " +
                    "for this app, turn it off so it can update your contacts."
            }
        }
    }

    /** Opens the phone app with the number filled in, so the call is one more tap. */
    private fun call(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
        } catch (e: ActivityNotFoundException) {
            contactsMessage = "No phone app was found to place the call."
        }
    }

    private fun openDndSettings() {
        val intents = listOf(Intent("android.settings.ZEN_MODE_SETTINGS"), Intent(Settings.ACTION_SETTINGS))
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Try the next one.
            }
        }
    }

    private fun updateAnnounce(newSettings: AnnounceSettings) {
        announce = newSettings
        store.saveAnnounceSettings(newSettings)
    }

    private fun hasAnnouncePermissions() = ANNOUNCE_PERMISSIONS.all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    private fun testAnnouncement() {
        announceMessage = "Playing a test announcement…"
        CallAnnouncer.speak(this, Announcement.text("Mom", null, null)) { spoke ->
            announceMessage = if (spoke) {
                null
            } else {
                "No text-to-speech voice is installed. Install one such as RHVoice or eSpeak NG " +
                    "from F-Droid, then choose it in Settings, Accessibility, Text-to-speech output."
            }
        }
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
