package io.github.secnewt.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.secnewt.dialer.calls.CallGroup
import io.github.secnewt.dialer.calls.CallHistory
import io.github.secnewt.dialer.calls.CallLogRepository
import io.github.secnewt.dialer.calls.CallStart
import io.github.secnewt.dialer.calls.Caller
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.Dialpad
import io.github.secnewt.dialer.calls.LiveCalls
import io.github.secnewt.dialer.calls.RingSettings
import io.github.secnewt.dialer.incall.CallManager
import io.github.secnewt.dialer.incall.InCallActivity
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.announce.Announcement
import io.github.secnewt.dialer.announce.CallAnnouncer
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.contacts.ContactsRepository
import io.github.secnewt.dialer.contacts.DndCalls
import io.github.secnewt.dialer.screening.BlockList
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.BlockSync
import io.github.secnewt.dialer.screening.SystemBlockList
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.dialer.screening.SpamSettingsStore
import io.github.secnewt.dialer.ui.BlockListScreen
import io.github.secnewt.dialer.ui.ContactDetailScreen
import io.github.secnewt.dialer.ui.ContactsPhotoLoader
import io.github.secnewt.dialer.ui.LocalPhotoLoader
import io.github.secnewt.dialer.ui.ContactsScreen
import io.github.secnewt.dialer.ui.DialpadIcon
import io.github.secnewt.dialer.ui.DialpadScreen
import io.github.secnewt.dialer.ui.NumberAction
import io.github.secnewt.dialer.ui.RecentsScreen
import io.github.secnewt.dialer.ui.ReturnToCallBar
import io.github.secnewt.dialer.ui.DialerTabBar
import io.github.secnewt.dialer.ui.FavoritesScreen
import io.github.secnewt.dialer.ui.SpamProtectionScreen
import io.github.secnewt.dialer.ui.SpamSettingsScreen
import io.github.secnewt.dialer.ui.SystemSetting
import io.github.secnewt.dialer.ui.Tab
import io.github.secnewt.dialer.ui.theme.DialerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** TABS shows Calls, Favorites or Contacts with the bottom bar; the others are full pages. */
private enum class Screen { TABS, SETTINGS, BLOCK_LIST, CONTACT, SPAM, DIALPAD }

private const val KEY_TONE_VOLUME = 80
private const val KEY_TONE_MILLIS = 120

/** Stands in for a number when the call is to voicemail. */
private const val VOICEMAIL = "voicemail"

private val ANNOUNCE_PERMISSIONS = arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS,
)

class MainActivity : ComponentActivity() {

    private val store by lazy { SpamSettingsStore(this) }
    private val contactsRepo by lazy { ContactsRepository(this) }
    private val photoLoader by lazy { ContactsPhotoLoader(this) }
    private val callLogRepo by lazy { CallLogRepository(this) }
    private val caller by lazy { Caller(this) }
    private val systemBlockList by lazy { SystemBlockList(this) }

    /** True once exact numbers are known to be on Android's block list as well. */
    private var systemBlockListSynced by mutableStateOf(false)

    private var callLogAccess by mutableStateOf(false)
    private var callGroups by mutableStateOf(emptyList<CallGroup>())
    private var callMessage by mutableStateOf<String?>(null)

    /** The number waiting for the person to answer the "make phone calls" question. */
    private var pendingCall: String? = null

    private val requestCallPhone = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Allowed: calls directly. Not allowed: opens the phone app with the number filled in.
        pendingCall?.let(::placeCall)
        pendingCall = null
    }

    private val requestCallLog = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        callMessage = if (granted) null else "Call history access wasn't allowed. If no question appeared, turn on " +
            "Call logs in Settings, Apps, ${getString(R.string.app_name)}, Permissions."
        refreshCallLog()
    }

    private var screeningRoleHeld by mutableStateOf(false)
    private var phoneAppRoleHeld by mutableStateOf(false)
    private var keypadTones by mutableStateOf(true)
    private var ringSettings by mutableStateOf(RingSettings())

    /** Made on the first key press and kept, so tones start without delay. */
    private var toneGenerator: ToneGenerator? = null

    /** Asked to become the phone app and didn't (often Android's restricted settings). */
    private var phoneAppDenied by mutableStateOf(false)

    /** A number another app asked to dial (a tel: link), waiting to be shown on the dialpad. */
    private var incomingDial by mutableStateOf<String?>(null)

    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Without it the call screen still opens; only the notification is missing.
    }
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
        incomingDial = dialRequest(intent)
        val roleManager = getSystemService(RoleManager::class.java)
        setContent {
            DialerTheme {
                CompositionLocalProvider(LocalPhotoLoader provides photoLoader) {
                    var screen by rememberSaveable { mutableStateOf(Screen.TABS) }
                    var tab by rememberSaveable { mutableStateOf(Tab.FAVORITES) }
                    var openContactId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var contactQuery by rememberSaveable { mutableStateOf("") }
                    var dialNumber by rememberSaveable { mutableStateOf("") }
                    val requestRole = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { refresh() }
                    val requestPhoneAppRole = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {
                        refresh()
                        phoneAppDenied = !phoneAppRoleHeld
                        if (phoneAppRoleHeld) askForNotifications()
                    }
                    val liveCalls by CallManager.calls.collectAsState()
                    val ongoingCall = LiveCalls.primary(liveCalls)?.takeIf { it.phase != CallPhase.ENDED }

                    LaunchedEffect(incomingDial) {
                        incomingDial?.let {
                            dialNumber = it
                            screen = Screen.DIALPAD
                            incomingDial = null
                        }
                    }
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
        refreshCallLog()
                        } else {
                            contactsMessage = "Contacts access wasn't allowed. If no question appeared, turn on " +
                                "Contacts in Settings, Apps, ${getString(R.string.app_name)}, Permissions."
                        }
                    }

                    BackHandler(enabled = screen != Screen.TABS || tab != Tab.FAVORITES) {
                        when (screen) {
                            Screen.TABS -> tab = Tab.FAVORITES
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
                    val enableScreening = {
                        requestRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                    }

                    when (screen) {
                        Screen.TABS -> Scaffold(
                            topBar = {
                                ongoingCall?.let { ReturnToCallBar(it, onClick = ::openCallScreen) }
                            },
                            bottomBar = { DialerTabBar(selected = tab, onSelect = { tab = it }) },
                            floatingActionButton = {
                                LargeFloatingActionButton(
                                    onClick = { screen = Screen.DIALPAD },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Icon(DialpadIcon, contentDescription = "Open dialpad", modifier = Modifier.size(36.dp))
                                }
                            },
                            contentWindowInsets = WindowInsets(0),
                        ) { padding ->
                            Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                                when (tab) {
                                    Tab.RECENTS -> RecentsScreen(
                                        groups = callGroups,
                                        hasAccess = callLogAccess,
                                        screened = recentCalls,
                                        roleHeld = screeningRoleHeld,
                                        observeOnly = settings.observeOnly,
                                        announceMode = announce.mode,
                                        onOpenSettings = openSettings,
                                        onOpenSpamProtection = { screen = Screen.SPAM },
                                        onEnableSpamProtection = enableScreening,
                                        onAllowAccess = { requestCallLog.launch(Manifest.permission.READ_CALL_LOG) },
                                        onCall = ::call,
                                        message = callMessage,
                                        onNumberAction = ::onNumberAction,
                                        isBlocked = { BlockList.match(blockRules, it) != null },
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
                            onRepeatChange = { updateAnnounce(announce.copy(repeat = it)) },
                            onTestAnnouncement = ::testAnnouncement,
                            isPhoneApp = phoneAppRoleHeld,
                            onMakePhoneApp = {
                                requestPhoneAppRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                            },
                            phoneAppDenied = phoneAppDenied,
                            onOpenSystemSetting = ::openSystemSetting,
                            ringSettings = ringSettings,
                            onRingSettingsChange = {
                                ringSettings = it
                                store.saveRingSettings(it)
                            },
                            keypadTones = keypadTones,
                            onKeypadTonesChange = {
                                keypadTones = it
                                store.saveKeypadTones(it)
                            },
                        )
                        Screen.SPAM -> SpamProtectionScreen(
                            roleHeld = screeningRoleHeld,
                            recentCalls = recentCalls,
                            observeOnly = settings.observeOnly,
                            blockRules = blockRules,
                            announceMode = announce.mode,
                            onEnable = enableScreening,
                            onOpenSettings = openSettings,
                            onBlock = { addRule(BlockRule.Number(it)) },
                            onUnblock = ::removeNumber,
                            onBack = { screen = Screen.TABS },
                        )
                        Screen.DIALPAD -> DialpadScreen(
                            number = dialNumber,
                            onNumberChange = { dialNumber = it },
                            contacts = contacts,
                            onCall = ::call,
                            onBack = { screen = Screen.TABS },
                            onVoicemail = { call(VOICEMAIL) },
                            onKeyTone = ::playKeyTone,
                        )
                        Screen.BLOCK_LIST -> BlockListScreen(
                            rules = blockRules,
                            onAdd = ::addRule,
                            onRemove = { rule -> saveRules(blockRules - rule) },
                            onBack = { screen = Screen.SETTINGS },
                            sharedWithAndroid = phoneAppRoleHeld && systemBlockListSynced && !settings.observeOnly,
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dialRequest(intent)?.let { incomingDial = it }
    }

    /**
     * The number in a "dial this" request from another app or a tel: link, or "" for an empty
     * dialpad; null when the app was simply opened.
     */
    private fun dialRequest(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_DIAL && intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return ""
        if (data.scheme != PhoneAccount.SCHEME_TEL) return ""
        return Dialpad.clean(data.schemeSpecificPart.orEmpty())
    }

    /** The dialpad's beep for [key], like the stock dialer. Quiet on silent or vibrate. */
    private fun playKeyTone(key: Char) {
        if (!keypadTones) return
        if (getSystemService(AudioManager::class.java).ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val tone = when (key) {
            in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (key - '0')
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        try {
            val generator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_DTMF, KEY_TONE_VOLUME).also { toneGenerator = it }
            generator.startTone(tone, KEY_TONE_MILLIS)
        } catch (e: RuntimeException) {
            // No tone hardware free right now; the key still works.
        }
    }

    override fun onDestroy() {
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    private fun openCallScreen() = startActivity(InCallActivity.intent(this))

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val roles = getSystemService(RoleManager::class.java)
        screeningRoleHeld = roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        phoneAppRoleHeld = roles.isRoleHeld(RoleManager.ROLE_DIALER)
        recentCalls = ScreeningLog(this).read()
        settings = store.settings()
        blockRules = store.blockRules()
        syncWithSystemBlockList()
        announce = store.announceSettings()
        keypadTones = store.keypadTones()
        ringSettings = store.ringSettings()
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

    /** Calls right away once allowed; asks for the "make phone calls" permission the first time. */
    private fun call(number: String) {
        if (caller.canCallDirectly()) {
            placeCall(number)
        } else {
            pendingCall = number
            requestCallPhone.launch(Manifest.permission.CALL_PHONE)
        }
    }

    /** The Recents menu: copy, add to contacts, text, edit before calling, or block a number. */
    private fun onNumberAction(action: NumberAction, number: String) {
        when (action) {
            NumberAction.COPY -> {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Phone number", number))
                // Android 13 and later show their own "Copied" message.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(this, "Number copied", Toast.LENGTH_SHORT).show()
                }
            }
            NumberAction.ADD_CONTACT -> openOrSay(
                Intent(Intent.ACTION_INSERT_OR_EDIT)
                    .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, number),
                "No contacts app was found.",
            )
            NumberAction.MESSAGE -> openOrSay(
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)),
                "No messaging app was found.",
            )
            NumberAction.EDIT -> incomingDial = number
            NumberAction.BLOCK -> {
                addRule(BlockRule.Number(number))
                callMessage = "Blocked. To undo, open Settings, Block list."
            }
        }
    }

    private fun openOrSay(intent: Intent, failure: String) {
        try {
            startActivity(intent)
            callMessage = null
        } catch (e: ActivityNotFoundException) {
            callMessage = failure
        }
    }

    private fun placeCall(number: String) {
        val result = if (number == VOICEMAIL) caller.callVoicemail() else caller.call(number)
        val message = if (result == CallStart.FAILED) "No phone app was found to place the call." else null
        callMessage = message
        contactsMessage = message
    }

    private fun refreshCallLog() {
        callLogAccess = callLogRepo.hasAccess()
        if (!callLogAccess) {
            callGroups = emptyList()
            return
        }
        lifecycleScope.launch {
            val calls = withContext(Dispatchers.IO) {
                try {
                    callLogRepo.load()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            callGroups = CallHistory.group(calls, ZoneId.systemDefault())
        }
    }

    private fun openDndSettings() = openFirst(Intent("android.settings.ZEN_MODE_SETTINGS"))

    /** Opens one of Android's settings screens; falls back to call settings, then Settings. */
    private fun openSystemSetting(setting: SystemSetting) {
        val intent = when (setting) {
            SystemSetting.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
            SystemSetting.CALLS -> Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)
            SystemSetting.VOICEMAIL -> Intent(TelephonyManager.ACTION_CONFIGURE_VOICEMAIL)
            SystemSetting.ACCESSIBILITY -> Intent(TelecomManager.ACTION_SHOW_CALL_ACCESSIBILITY_SETTINGS)
            SystemSetting.TTS -> Intent("com.android.settings.TTS_SETTINGS")
            SystemSetting.APP_INFO ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        }
        openFirst(intent, Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS))
    }

    private fun openFirst(vararg preferred: Intent) {
        val intents = preferred.toList() + Intent(Settings.ACTION_SETTINGS)
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
        val startedBlocking = settings.observeOnly && !newSettings.observeOnly
        settings = newSettings
        store.saveSettings(newSettings)
        if (startedBlocking) syncWithSystemBlockList()
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
        val change = BlockSync.change(blockRules, rules)
        blockRules = rules
        store.saveBlockRules(rules)
        // As the phone app, exact numbers also go on Android's own block list (not in observe
        // only mode, where every call still rings).
        if (phoneAppRoleHeld && (change.added.isNotEmpty() || change.removed.isNotEmpty())) {
            val enforce = !settings.observeOnly
            lifecycleScope.launch(Dispatchers.IO) {
                if (!systemBlockList.isAvailable()) return@launch
                if (enforce) change.added.forEach(systemBlockList::add)
                change.removed.forEach(systemBlockList::remove)
            }
        }
    }

    /**
     * Makes this app's exact numbers and Android's block list match, both ways, so numbers
     * blocked before (in the stock phone app) show up here and vice versa. In observe-only
     * mode nothing is sent to Android, since those calls are meant to keep ringing.
     */
    private fun syncWithSystemBlockList() {
        if (!phoneAppRoleHeld) return
        val enforce = !settings.observeOnly
        lifecycleScope.launch {
            val fromAndroid = withContext(Dispatchers.IO) {
                if (!systemBlockList.isAvailable()) return@withContext null
                val plan = BlockSync.plan(blockRules, systemBlockList.numbers())
                if (enforce) plan.toAndroid.forEach(systemBlockList::add)
                plan.toApp
            } ?: return@launch
            systemBlockListSynced = true
            if (fromAndroid.isNotEmpty()) {
                val merged = blockRules + fromAndroid.map { BlockRule.Number(it) }
                blockRules = merged
                store.saveBlockRules(merged)
            }
        }
    }
}
