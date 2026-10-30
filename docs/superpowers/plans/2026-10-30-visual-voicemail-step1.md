# Visual Voicemail, Step 1 (prove it on the phone) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Behind a "Visual voicemail" setting that starts off, activate visual voicemail with Ting by SMS, log in to Ting's voicemail server over mobile data, and show each step as a plain status line, so the owner can install it and report whether it works.

**Architecture:** A new `voicemail` package. Pure Kotlin pieces (carrier config → SMS text, reply parsing → next action, a minimal IMAP probe, a prefs-backed store) carry the logic and are unit-tested. A thin Android layer (`NewtVoicemailService`, `VoicemailActivator`, `CellularNetwork`) wires them to `TelephonyManager`, `ConnectivityManager` and the system's `VisualVoicemailService` binding. A Settings section turns it on or off and shows the status lines.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 and the Newt library, coroutines (already on the classpath), Robolectric + Roborazzi tests. **No new dependencies in this step.**

**Spec:** `docs/superpowers/specs/2026-10-30-visual-voicemail-design.md` (this plan covers "Order of work" step 1 only; steps 2–4 get their own plan after the phone result).

## Global Constraints

- minSdk 30, targetSdk 35, as in `app/build.gradle.kts`. No new libraries.
- Visual voicemail is **off by default**. Nothing is sent, and no network is requested, until the owner turns it on.
- Network access only from `io.github.secnewt.dialer.voicemail`, over **cellular only**, never Wi-Fi.
- New permissions: exactly `android.permission.INTERNET` and `android.permission.CHANGE_NETWORK_STATE`, each with a comment in `config/allowed-permissions.txt` and the manifest saying it's for visual voicemail only.
- The IMAP username and password are never shown on screen, never logged, and never put in a status line.
- UI follows `NEWT_STYLE.md`: buttons at least 56 dp, words next to icons, 7:1 contrast, works at 200% text, one plain sentence per setting.
- Screenshot and accessibility tests in light, dark and 200% text, via the existing `SettingsScreensTest` `render` helper.
- Commit messages in the repo's style (plain-English summary); end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Facts the code relies on (checked Oct 30, 2026 against AOSP)

- T-Mobile US carrier config (`CarrierConfig/assets/carrier_config_carrierid_1_T-Mobile-US.xml`): `vvm_type_string = vvm_type_cvvm`, `vvm_destination_number_string = 122`, `vvm_port_number_int = 1808`. Android has no Ting-specific config, so a legacy Ting SIM may report these values or none at all.
- CVVM requests (AOSP `OmtpCvvmMessageSender`): `Activate:dt=6`, `Deactivate:dt=6`, `Status:dt=6`.
- OMTP v1.1 requests (AOSP `OmtpStandardMessageSender`): `STATUS`, `Activate:pv=11;ct=<clientType>`, `Deactivate:pv=11;ct=<clientType>`. Use client type `newt.phone` (only used if the SIM reports `vvm_type_omtp`).
- Reply SMS fields (AOSP `OmtpConstants`): prefix `STATUS` or `SYNC`. STATUS fields: `st` (N new, R ready, P provisioned, U unknown, B blocked), `rc` (0 success, 1 system error, 2 subscriber error, 3 mailbox unknown, 4 not activated, 5 not provisioned, 6 client unknown, 7 mailbox not initialized), `srv` IMAP host, `ipt` IMAP port, `u` user, `pw` password. SYNC fields: `ev` (NM new message, MBU mailbox update, GU greeting update), `c` message count.
- Android parses matching replies itself and hands them to the default phone app's `VisualVoicemailService.onSmsReceived` as `VisualVoicemailSms` (`prefix` and `fields`), once a filter is set with `TelephonyManager.setVisualVoicemailSmsFilterSettings`. The client prefix is `//VVM`.

## Review Focus

1. **Credentials leaking into status text.** A status line or exception message that echoes the username or password. Expected: never. The test is in Task 3 (IMAP error text) and Task 4 (store).
2. **GrapheneOS's Network permission is off.** Socket creation fails with `EACCES`/"Permission denied". Expected: the line "Can't connect: Newt Phone's Network permission is off in Android settings.", not a crash. The test is in Task 3.
3. **Ting never replies, or keeps replying "not ready".** Expected: at most one `Activate` per time the setting is turned on, never a loop of texts. The test is in Task 2.
4. **The setting is turned off after activation.** Expected: `Deactivate` is sent so Ting goes back to ordinary voicemail alerts, the SMS filter is cleared, and saved credentials are erased. The tests are in Tasks 2 and 4.
5. **Not the default phone app, no SIM, or airplane mode.** Expected: a plain status line, no crash, no SecurityException reaching the user. The test is in Task 2 (`startAction`).

---

### Task 1: Carrier settings and request texts

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/VvmConfig.kt`
- Test: `app/src/test/java/io/github/secnewt/dialer/voicemail/VvmConfigTest.kt`

**Interfaces:**
- Produces:
  - `enum class VvmType { OMTP, CVVM }`
  - `enum class ConfigSource { SIM, TMOBILE_DEFAULTS }`
  - `data class VvmConfig(val type: VvmType, val destinationNumber: String, val port: Int, val ssl: Boolean, val source: ConfigSource)` with `fun activateText(): String`, `fun deactivateText(): String`, `fun statusText(): String`
  - `fun vvmConfigOf(values: Map<String, Any?>, simOperator: String?): VvmConfig?`. `values` holds the carrier config keys (`vvm_type_string`, `vvm_destination_number_string`, `vvm_port_number_int`, `vvm_ssl_enabled_bool`). Returns null when there's no usable config.
  - `const val VVM_CLIENT_PREFIX = "//VVM"`
  - `val TMOBILE_US_OPERATORS: Set<String>` = `310160, 310200, 310210, 310220, 310230, 310240, 310250, 310260, 310270, 310310, 310490, 310660, 310800`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `cvvm from the SIM`() {
    val c = vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_cvvm", "vvm_destination_number_string" to "122", "vvm_port_number_int" to 1808), "310260")!!
    assertEquals(VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.SIM), c)
    assertEquals("Activate:dt=6", c.activateText())
    assertEquals("Deactivate:dt=6", c.deactivateText())
    assertEquals("Status:dt=6", c.statusText())
}
@Test fun `omtp texts`() {
    val c = vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_omtp", "vvm_destination_number_string" to "5551234", "vvm_port_number_int" to 0), null)!!
    assertEquals("Activate:pv=11;ct=newt.phone", c.activateText())
    assertEquals("Deactivate:pv=11;ct=newt.phone", c.deactivateText())
    assertEquals("STATUS", c.statusText())
}
@Test fun `empty SIM config on a T-Mobile network falls back to T-Mobile defaults`() {
    assertEquals(VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.TMOBILE_DEFAULTS), vvmConfigOf(emptyMap(), "310260"))
}
@Test fun `empty config elsewhere is null`() { assertNull(vvmConfigOf(emptyMap(), "311480")); assertNull(vvmConfigOf(emptyMap(), null)) }
@Test fun `unknown type such as vvm3 is null`() { assertNull(vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_vvm3", "vvm_destination_number_string" to "900080006200"), "311480")) }
@Test fun `missing destination number is null even with a type`() { assertNull(vvmConfigOf(mapOf("vvm_type_string" to "vvm_type_cvvm"), "311480")) }
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*VvmConfigTest*'`
Expected: compilation failure (`vvmConfigOf` not defined).

- [ ] **Step 3: Implement `VvmConfig.kt`**

It's pure Kotlin with no Android types, so it's testable without Robolectric. A SIM config with a type but no destination number counts as unusable; fall back to T-Mobile defaults only when the operator is in `TMOBILE_US_OPERATORS`.

- [ ] **Step 4: Run the tests and confirm they pass**

Same command. Expected: PASS.

- [ ] **Step 5: Commit**: "Visual voicemail: carrier settings and request texts"

---

### Task 2: Replies, next action, and status lines

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/VvmFlow.kt`
- Test: `app/src/test/java/io/github/secnewt/dialer/voicemail/VvmFlowTest.kt`

**Interfaces:**
- Consumes: `VvmConfig` (Task 1).
- Produces:
  - `data class StatusReply(val provisioning: String, val returnCode: String, val host: String?, val imapPort: Int?, val user: String?, val password: String?)`, with `companion fun from(fields: Map<String, String>): StatusReply` and `override fun toString()` that **leaves out user and password**
  - `sealed interface VvmAction { data object SendStatus; data object SendActivate; data class Login(val reply: StatusReply); data class Stop(val line: String) }`
  - `fun startAction(isPhoneApp: Boolean, hasSim: Boolean, config: VvmConfig?, simOperator: String?): VvmAction`
  - `fun actionForStatus(reply: StatusReply, activateAlreadySent: Boolean): VvmAction`
  - `fun actionForSync(event: String?): VvmAction` (`NM` and `MBU` → `SendStatus`, so the probe reruns with fresh credentials; anything else → `Stop("Ting sent an update: $event.")`)
  - `object VvmText`, holding the status-line copy used by every later task:

```kotlin
object VvmText {
    const val OFF = "Visual voicemail is off."
    const val NOT_PHONE_APP = "Make Newt Phone your phone app first."
    const val NO_SIM = "No SIM card found."
    fun noConfig(op: String?) = "This SIM has no visual voicemail settings (network ${op ?: "unknown"})."
    fun config(c: VvmConfig) = "Carrier settings: ${c.type}, number ${c.destinationNumber}, port ${c.port} (" +
        (if (c.source == ConfigSource.SIM) "from the SIM" else "T-Mobile defaults") + ")."
    const val STATUS_SENT = "Asked Ting for voicemail details (text sent)."
    const val ACTIVATE_SENT = "Asked Ting to turn on visual voicemail (text sent)."
    fun sendFailed(why: String) = "Couldn't send the text to Ting: $why"
    fun replied(r: StatusReply) = "Ting answered: status ${r.provisioning}, code ${r.returnCode}."
    const val BLOCKED = "Ting says visual voicemail is blocked on this line."
    const val STILL_NOT_READY = "Ting still says visual voicemail isn't ready. Turn the setting off and on to try again later."
    const val NO_SERVER = "Ting answered but didn't say which server to use."
    const val CONNECTING = "Connecting over mobile data…"
    const val NO_DATA = "No mobile data connection (waited 30 seconds)."
    const val NETWORK_PERMISSION = "Can't connect: Newt Phone's Network permission is off in Android settings."
    fun loggedIn(count: Int) = "Logged in. $count voicemail${if (count == 1) "" else "s"} on Ting's server."
    fun loginRefused(server: String) = "Ting refused the login: $server"
    fun failed(why: String) = "Connection failed: $why"
    const val WAITING = "Ting hasn't answered the text yet."
    const val NEW_VOICEMAIL = "Ting says there's a new voicemail."
}
```

- [ ] **Step 1: Write the failing tests**

```kotlin
private val cvvm = VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.SIM)
private fun reply(st: String, rc: String = "0", srv: String? = "vvm.example", ipt: String? = "143") =
    StatusReply.from(buildMap { put("st", st); put("rc", rc); srv?.let { put("srv", it) }; ipt?.let { put("ipt", it) }; put("u", "user1@ting"); put("pw", "s3cret") })

@Test fun `not the phone app stops first`() = assertEquals(VvmAction.Stop(VvmText.NOT_PHONE_APP), startAction(false, true, cvvm, "310260"))
@Test fun `no SIM stops`() = assertEquals(VvmAction.Stop(VvmText.NO_SIM), startAction(true, false, null, null))
@Test fun `no config stops with the operator`() = assertEquals(VvmAction.Stop(VvmText.noConfig("311480")), startAction(true, true, null, "311480"))
@Test fun `ready start asks for status`() = assertEquals(VvmAction.SendStatus, startAction(true, true, cvvm, "310260"))
@Test fun `ready reply logs in`() { val r = reply("R"); assertEquals(VvmAction.Login(r), actionForStatus(r, false)) }
@Test fun `new or provisioned activates once`() {
    assertEquals(VvmAction.SendActivate, actionForStatus(reply("N"), false))
    assertEquals(VvmAction.SendActivate, actionForStatus(reply("P"), false))
    assertEquals(VvmAction.Stop(VvmText.STILL_NOT_READY), actionForStatus(reply("N"), true))
}
@Test fun `blocked stops`() = assertEquals(VvmAction.Stop(VvmText.BLOCKED), actionForStatus(reply("B"), false))
@Test fun `ready without a server stops`() = assertEquals(VvmAction.Stop(VvmText.NO_SERVER), actionForStatus(reply("R", srv = null), false))
@Test fun `missing imap port defaults to 143`() = assertEquals(143, reply("R", ipt = null).imapPort)
@Test fun `toString never shows credentials`() { val s = reply("R").toString(); assertFalse(s.contains("user1")); assertFalse(s.contains("s3cret")) }
@Test fun `sync new message rechecks`() = assertEquals(VvmAction.SendStatus, actionForSync("NM"))
@Test fun `config line names the source`() = assertEquals("Carrier settings: CVVM, number 122, port 1808 (from the SIM).", VvmText.config(cvvm))
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*VvmFlowTest*'`. Expected: compilation failure.

- [ ] **Step 3: Implement `VvmFlow.kt`**

Order of checks in `startAction`: phone app, then SIM, then config. In `actionForStatus`: `B` → blocked; `R` → `Login`, or `NO_SERVER` when `host` is null; `N`/`P`/`U` → `SendActivate` the first time, `STILL_NOT_READY` after. A non-`0` `rc` with `R` still tries to log in, since the login result says more than the code does.

- [ ] **Step 4: Run the tests and confirm they pass.** Same command. Expected: PASS.

- [ ] **Step 5: Commit**: "Visual voicemail: read Ting's replies and decide the next step"

---

### Task 3: IMAP login probe

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/ImapProbe.kt`
- Test: `app/src/test/java/io/github/secnewt/dialer/voicemail/ImapProbeTest.kt`

**Interfaces:**
- Consumes: `VvmText` (Task 2).
- Produces:
  - `sealed interface ProbeResult { data class LoggedIn(val messages: Int); data class Refused(val serverText: String); data class Failed(val line: String) }`
  - `fun probeMailbox(input: BufferedReader, output: Writer, user: String, password: String): ProbeResult`. This is the conversation only, over streams the caller opens.
  - `fun probeLine(result: ProbeResult): String`, which maps to the `VvmText` lines (`loggedIn`, `loginRefused`, `failed`).
  - `fun connectFailureLine(e: Exception): String`, which returns `VvmText.NETWORK_PERMISSION` when the exception or its cause message contains `EACCES` or `Permission denied`, otherwise `VvmText.failed(e.javaClass.simpleName)`. It uses the class name only, because socket messages can include the host and port but should never include credentials.

The conversation is: read the greeting, `a1 LOGIN "<user>" "<password>"` (quote and backslash-escape `"` and `\`), `a2 SELECT INBOX` (count from the `* <n> EXISTS` line), `a3 LOGOUT`. Lines end in CRLF.

- [ ] **Step 1: Write the failing tests**

```kotlin
private fun run(server: String, user: String = "u1", pw: String = "p\"w"): Pair<ProbeResult, String> {
    val out = StringWriter()
    return probeMailbox(BufferedReader(StringReader(server)), out, user, pw) to out.toString()
}
@Test fun `logs in and counts messages`() {
    val (r, sent) = run("* OK ready\r\na1 OK logged in\r\n* 3 EXISTS\r\n* 0 RECENT\r\na2 OK selected\r\n* BYE\r\na3 OK\r\n")
    assertEquals(ProbeResult.LoggedIn(3), r)
    assertTrue(sent.startsWith("a1 LOGIN \"u1\" \"p\\\"w\"\r\na2 SELECT INBOX\r\n"))
}
@Test fun `refused login keeps the server text but not the password`() {
    val (r, _) = run("* OK ready\r\na1 NO [AUTHENTICATIONFAILED] Invalid credentials\r\n")
    assertEquals(ProbeResult.Refused("[AUTHENTICATIONFAILED] Invalid credentials"), r)
    assertFalse(probeLine(r).contains("p\"w"))
}
@Test fun `connection closed early fails plainly`() = assertTrue(run("* OK ready\r\n").first is ProbeResult.Failed)
@Test fun `permission denied socket error explains the Network permission`() =
    assertEquals(VvmText.NETWORK_PERMISSION, connectFailureLine(java.net.SocketException("socket failed: EACCES (Permission denied)")))
@Test fun `other socket errors give the class name only`() =
    assertEquals(VvmText.failed("ConnectException"), connectFailureLine(java.net.ConnectException("10.1.2.3:143 user1@ting refused")))
```

- [ ] **Step 2: Run them and confirm they fail.** `./gradlew :app:testDebugUnitTest --tests '*ImapProbeTest*'`. Expected: compilation failure.
- [ ] **Step 3: Implement `ImapProbe.kt`.** Skip untagged lines while waiting for a tagged reply. A tagged `NO`/`BAD` to `LOGIN` → `Refused` with the text after the status word. EOF → `Failed(VvmText.failed("the server closed the connection"))`.
- [ ] **Step 4: Run the tests and confirm they pass.**
- [ ] **Step 5: Commit**: "Visual voicemail: check the login to Ting's voicemail server"

---

### Task 4: Saved state

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/VoicemailStore.kt`
- Test: `app/src/test/java/io/github/secnewt/dialer/voicemail/VoicemailStoreTest.kt`

**Interfaces:**
- Produces: `class VoicemailStore(prefs: SharedPreferences)` (plus a `Context` constructor using its own prefs file `"voicemail"`, `MODE_PRIVATE`, following `SpamSettingsStore`):
  - `var enabled: Boolean` (default false)
  - `var activateSent: Boolean`
  - `fun saveCredentials(reply: StatusReply)` / `fun credentials(): StatusReply?` / `fun clearCredentials()`
  - `data class StatusLine(val atMillis: Long, val text: String)`; `fun addLine(text: String, nowMillis: Long)`; `fun lines(): List<StatusLine>` (newest last, keeps the last 20); `fun clearLines()`
  - `fun turnOff()`: sets `enabled = false` and `activateSent = false`, clears credentials, and replaces the lines with a single `VvmText.OFF`.
- Notes: credentials live only in this app-private prefs file. `allowBackup="false"` is already set.

- [ ] **Step 1: Write the failing tests** (Robolectric, like `SpamSettingsStoreTest`): off by default; enabled survives reload; credentials round-trip and `turnOff()` erases them; `lines()` keeps only the newest 20, in order; **`addLine` rejects text containing the saved user or password** by replacing it with `"(hidden)"`, a backstop for Review Focus 1.
- [ ] **Step 2: Run and confirm they fail.** `./gradlew :app:testDebugUnitTest --tests '*VoicemailStoreTest*'`
- [ ] **Step 3: Implement.** Store lines as a JSON array (`org.json`, as `SpamSettingsStore` does).
- [ ] **Step 4: Run and confirm they pass.**
- [ ] **Step 5: Commit**: "Visual voicemail: remember the setting and the status lines"

---

### Task 5: Talking to Android: service, SMS, mobile data

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/CellularNetwork.kt`
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/VoicemailActivator.kt`
- Create: `app/src/main/java/io/github/secnewt/dialer/voicemail/NewtVoicemailService.kt`
- Modify: `app/src/main/AndroidManifest.xml` (permissions + service; fix the "no internet access" comment at line 6)
- Modify: `config/allowed-permissions.txt`
- Create: `app/src/test/java/io/github/secnewt/dialer/voicemail/NetworkUseTest.kt`

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces:
  - `suspend fun <T> withCellularNetwork(context: Context, subId: Int, timeoutMillis: Long = 30_000, block: (Network) -> T): T?`. Requests `TRANSPORT_CELLULAR` + `NET_CAPABILITY_INTERNET` with `TelephonyNetworkSpecifier(subId)`, runs `block` on `Dispatchers.IO` once it's available, always unregisters, and returns null on timeout.
  - `class VoicemailActivator(context: Context, store: VoicemailStore)` with `suspend fun start()`, `suspend fun onSms(prefix: String, fields: Map<String, String>)`, `suspend fun stop()`. These are called by the service and by the settings toggle.
  - `NewtVoicemailService : VisualVoicemailService`: `onCellServiceConnected` → `start()` if `store.enabled`; `onSmsReceived` → `onSms(sms.prefix, sms.fields as map)`; `onSimRemoved`/`onStopped` → nothing beyond `task.finish()`. Always call `task.finish()`, including on errors.

- [ ] **Step 1: Write the failing `NetworkUseTest`**

It walks `src/main/java` (working dir is the `app` module) and fails if any `.kt` file **outside** `io/github/secnewt/dialer/voicemail/` mentions any of `java.net.`, `javax.net.`, `HttpURLConnection`, `socketFactory`, `openConnection`, `ConnectivityManager.requestNetwork`, `requestNetwork(`. The failure message lists file:line. Also assert that the walk found more than 40 files, so a wrong path can't pass silently.

- [ ] **Step 2: Run and confirm it passes before the new code exists, then add a temporary `java.net.URL` import in `ui/Components.kt` and confirm it fails. Remove the import.**

Run: `./gradlew :app:testDebugUnitTest --tests '*NetworkUseTest*'`

- [ ] **Step 3: Implement the three files**

`start()`:
- Not enabled → return.
- Find the voice subscription: `SubscriptionManager.getDefaultVoiceSubscriptionId()`, then `TelephonyManager.createForSubscriptionId`.
- `isPhoneApp` = `RoleManager.isRoleHeld(ROLE_DIALER)`.
- `config` = `vvmConfigOf(carrierConfigManager.getConfigForSubId(subId) keys, tm.simOperator)`.
- Apply `startAction(...)` and log each step through `store.addLine`, logging `VvmText.config(c)` first.
- `SendStatus` → set the SMS filter (`VisualVoicemailSmsFilterSettings.Builder().setClientPrefix(VVM_CLIENT_PREFIX).setDestinationPort(DESTINATION_PORT_ANY)`), then `tm.sendVisualVoicemailSms(destinationNumber, port, statusText(), sentIntent = null)`, and log `STATUS_SENT`. Catch `SecurityException`/`IllegalArgumentException` → `sendFailed(e.javaClass.simpleName)`.

`onSms`:
- `STATUS` → `StatusReply.from`, log `replied`, apply `actionForStatus(reply, store.activateSent)`.
  - `SendActivate`: send `activateText()`, set `activateSent`, log `ACTIVATE_SENT`.
  - `Login`: `saveCredentials`, log `CONNECTING`, then `withCellularNetwork { net -> net.socketFactory.createSocket(host, port)` (wrapped in `SSLSocketFactory.getDefault()` when `config.ssl`), with a 20-second read timeout, then `probeMailbox` over its streams`}`. Log `probeLine(result)`, `NO_DATA` when null, or `connectFailureLine(e)` on exception.
- `SYNC` → log `NEW_VOICEMAIL`, apply `actionForSync`.

`stop()`: send `deactivateText()` (ignore failures), `setVisualVoicemailSmsFilterSettings(null)`, `store.turnOff()`.

Manifest:

```xml
<!-- Visual voicemail only (off until turned on in Settings): reach Ting's voicemail
     server over mobile data. No other part of the app uses the network; a test enforces it. -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
...
<!-- Android hands visual voicemail texts from Ting to this service while the app is the
     default phone app. -->
<service android:name=".voicemail.NewtVoicemailService" android:exported="true"
    android:permission="android.permission.BIND_VISUAL_VOICEMAIL_SERVICE">
    <intent-filter><action android:name="android.telephony.VisualVoicemailService" /></intent-filter>
</service>
```

Line 6's comment becomes: "Only visual voicemail uses the network, and only to reach Ting; numbers and names never leave the phone." Add both permissions to `allowed-permissions.txt` under a `# Visual voicemail (off by default): reach Ting's voicemail server over mobile data. Nothing else in the app goes online; NetworkUseTest enforces it.` comment.

- [ ] **Step 4: Run the whole suite and the build.** `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Expected: BUILD SUCCESSFUL, `NetworkUseTest` PASS.
- [ ] **Step 5: Commit**: "Visual voicemail: activate with Ting by text and test the login over mobile data"

---

### Task 6: Settings section

**Files:**
- Create: `app/src/main/java/io/github/secnewt/dialer/ui/VoicemailSettings.kt`
- Modify: `app/src/main/java/io/github/secnewt/dialer/ui/SpamSettingsScreen.kt:89-110` (new params) and `:154` (new section after Ringing)
- Modify: `app/src/main/java/io/github/secnewt/dialer/MainActivity.kt` (state + `Screen.SETTINGS` wiring around line 360)
- Test: `app/src/test/java/io/github/secnewt/dialer/ui/SettingsScreensTest.kt`, plus `VoicemailSettingsTest` for `waitingHint`

**Interfaces:**
- Consumes: `VoicemailStore.StatusLine`, `VvmText` (Tasks 2 and 4), `VoicemailActivator` (Task 5).
- Produces:
  - `@Composable fun VoicemailSection(enabled: Boolean, lines: List<StatusLine>, nowMillis: Long, onEnabledChange: (Boolean) -> Unit)`
  - `fun waitingHint(lines: List<StatusLine>, nowMillis: Long): String?`, which returns `VvmText.WAITING` when the newest line is `STATUS_SENT` or `ACTIVATE_SENT` and is more than 2 minutes old.
  - New `SpamSettingsScreen` params: `voicemailEnabled: Boolean = false`, `voicemailLines: List<StatusLine> = emptyList()`, `onVoicemailEnabledChange: (Boolean) -> Unit = {}`.

Section copy: heading "Visual voicemail". The switch row is titled "Visual voicemail" with the detail "Download your voicemails and read them here. Uses mobile data to reach Ting, nothing else." When on, a "Status" list appears below it: each line with its time (`quietTimeLabel`-style short time) in body text, newest at the bottom, followed by the `waitingHint` if there is one. Reuse the `SwitchRow` from `RingingSettings.kt` by making it `internal` rather than copying it.

- [ ] **Step 1: Write the failing tests**
  - `waitingHint` cases: null when empty; null when sent under 2 minutes ago; `WAITING` at 2 minutes plus 1 ms; null when the newest line is `loggedIn(3)`.
  - Screenshots via `render(...)`: `voicemail_off` (dark), `voicemail_on_status` (dark, light, and fontScale 2f) with the lines `config`, `STATUS_SENT`, `replied`, `CONNECTING`, `loggedIn(3)`.
  - `onNodeWithText("Visual voicemail").performClick()` calls `onEnabledChange(true)`.
- [ ] **Step 2: Run and confirm they fail.** `./gradlew :app:testDebugUnitTest --tests '*SettingsScreensTest*' --tests '*VoicemailSettingsTest*'`
- [ ] **Step 3: Implement.** In `MainActivity`, `voicemailStore` is `lazy`, and `voicemailEnabled`/`voicemailLines` are `mutableStateOf`. Refresh them in `onResume`, and every 2 seconds while Settings is showing and voicemail is on (`LaunchedEffect` loop), since the service writes from another component. When toggled: on → `store.enabled = true`, `lifecycleScope.launch { activator.start() }`; off → `lifecycleScope.launch { activator.stop() }`.
- [ ] **Step 4: Run and confirm they pass; record new screenshots** (`./gradlew :app:recordRoborazziDebug`) and look at them at 200%.
- [ ] **Step 5: Commit**: "Visual voicemail: on/off setting with step-by-step status"

---

### Task 7: Docs, then the phone test

**Files:**
- Modify: `README.md:18,27` and `SECURITY.md:17`: "no internet" becomes "internet only for visual voicemail, off by default, cellular only, enforced by the permission check and `NetworkUseTest`".
- Modify: `PLAN.md`: line 30's "The app never gets the internet permission" gets "except for visual voicemail (approved Oct 29, 2026), and only that code may use it". Line 126 (GrapheneOS Network permission) notes that it must be on for visual voicemail. Line 71's stretch idea points to the spec.

- [ ] **Step 1: Edit the three docs and commit**: "Docs: internet is for visual voicemail only"
- [ ] **Step 2: Push the branch, open a PR, and wait for CI to pass** (tests, screenshots, lint, permission audit, emulator call test). Expected: all green. The permission audit shows `INTERNET` and `CHANGE_NETWORK_STATE` as allowed.
- [ ] **Step 3: The owner says "merge it"; the signed release installs through Obtainium.** The setting is off by default, so the release changes nothing until it's turned on.
- [ ] **Step 4: Phone check (the owner).** If GrapheneOS asks, allow Newt Phone's Network permission. Then turn on Settings → Visual voicemail and report the status lines.
  - **Success** is `Logged in. N voicemails on Ting's server.` That leads to the Step 2 plan.
  - **Failure** at any line is recorded in `PLAN.md` under Open questions, with the exact line. If the cause is Ting's network, visual voicemail stops there, as the spec says.

---

## After Step 1

Steps 2–4 of the spec (download and store, Voicemail tab and message screen, notification, transcription, cleanup) get their own plan once the phone result is in. The working login code and the status lines from this step are what that plan builds on.
