package io.github.secnewt.dialer.screening

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SpamSettingsStoreTest {

    private val store = SpamSettingsStore(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `defaults when nothing is saved`() {
        assertEquals(SpamSettings(), store.settings())
        assertTrue(store.blockRules().isEmpty())
    }

    @Test
    fun `settings survive a save and load`() {
        val settings = SpamSettings(observeOnly = false, myNumber = "5550142290")
            .withLevel(ProtectionLevel.STRICT)
            .withHiddenNumbers(CallAction.RING)
        store.saveSettings(settings)
        assertEquals(settings, store.settings())
    }

    @Test
    fun `announcement is off by default and survives a save and load`() {
        assertEquals(AnnounceSettings(), store.announceSettings())
        val settings = AnnounceSettings(mode = AnnounceMode.HEADPHONES_ONLY, quietDuringDnd = false)
        store.saveAnnounceSettings(settings)
        assertEquals(settings, store.announceSettings())
    }

    @Test
    fun `block rules survive a save and load`() {
        val rules = listOf(BlockRule.Number("(555) 019-7731"), BlockRule.StartsWith("800555"))
        store.saveBlockRules(rules)
        assertEquals(rules, store.blockRules())
    }
}
