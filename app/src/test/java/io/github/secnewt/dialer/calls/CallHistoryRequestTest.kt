package io.github.secnewt.dialer.calls

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallHistoryRequestTest {

    @Test
    fun theMissedCallNotificationOpensTheCallHistory() {
        assertTrue(CallHistoryRequest.matches("android.intent.action.VIEW", "vnd.android.cursor.dir/calls", null))
        assertTrue(CallHistoryRequest.matches("android.intent.action.VIEW", null, "content://call_log/calls"))
    }

    @Test
    fun telLinksAndPlainOpensAreNotCallHistory() {
        assertFalse(CallHistoryRequest.matches("android.intent.action.VIEW", null, "tel:5550199"))
        assertFalse(CallHistoryRequest.matches("android.intent.action.DIAL", "vnd.android.cursor.dir/calls", null))
        assertFalse(CallHistoryRequest.matches("android.intent.action.MAIN", null, null))
    }
}
