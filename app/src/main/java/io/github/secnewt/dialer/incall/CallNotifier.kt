package io.github.secnewt.dialer.incall

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import io.github.secnewt.dialer.R
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.LiveCall
import io.github.secnewt.dialer.calls.LiveCalls
import io.github.secnewt.dialer.ui.callTitle

/**
 * The notification for the call in progress: Answer and Decline while it rings, Hang up once
 * it's going. Tapping it opens the call screen.
 */
class CallNotifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        // Android plays the ringtone, so the notification itself stays silent.
        val incoming = NotificationChannel(INCOMING_CHANNEL, "Incoming calls", NotificationManager.IMPORTANCE_HIGH)
        incoming.setSound(null, null)
        incoming.enableVibration(false)
        val incomingQuiet = NotificationChannel(
            INCOMING_QUIET_CHANNEL, "Incoming calls (call screen open)", NotificationManager.IMPORTANCE_LOW,
        )
        val ongoing = NotificationChannel(ONGOING_CHANNEL, "Calls in progress", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannels(listOf(incoming, incomingQuiet, ongoing))
    }

    /** The ringing call we've posted about, and whether its call screen has been seen. */
    private var ringingId: String? = null
    private var screenSeen = false
    private var postedQuietly = false

    /**
     * Shows the right notification for [calls]. While the call screen is open, a ringing call's
     * notification stays in the shade without popping up; once you leave the screen it pops up
     * again. The full-screen launch is used only until the screen has been seen, so pressing
     * power to silence a call doesn't wake the screen again.
     */
    fun update(calls: List<LiveCall>, screenShowing: Boolean) {
        val main = LiveCalls.primary(calls)
        if (main == null || main.phase == CallPhase.ENDED) {
            cancel()
            return
        }
        if (!canNotify()) return
        val notification = if (main.phase == CallPhase.RINGING) {
            if (main.id != ringingId) {
                ringingId = main.id
                screenSeen = false
            }
            if (screenShowing) screenSeen = true
            // Going from quiet to loud: start over so it pops up instead of updating silently.
            if (!screenShowing && postedQuietly) manager.cancel(NOTIFICATION_ID)
            postedQuietly = screenShowing
            buildIncoming(main, quiet = screenShowing, fullScreen = !screenSeen)
        } else {
            ringingId = null
            postedQuietly = false
            buildOngoing(main)
        }
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notifications were turned off; the call screen still works.
        }
    }

    fun cancel() {
        ringingId = null
        postedQuietly = false
        manager.cancel(NOTIFICATION_ID)
    }

    /** Android 13 and later ask before an app may show notifications. */
    private fun canNotify(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        manager.areNotificationsEnabled()
    }

    private fun openScreen(): PendingIntent = PendingIntent.getActivity(
        context, 0, InCallActivity.intent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun buildIncoming(call: LiveCall, quiet: Boolean, fullScreen: Boolean): Notification {
        val open = openScreen()
        val builder = Notification.Builder(context, if (quiet) INCOMING_QUIET_CHANNEL else INCOMING_CHANNEL)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(callTitle(call))
            .setContentText(call.spamLabel ?: "Incoming call")
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(action("Decline", CallActionReceiver.DECLINE, call.id, 1))
            .addAction(action("Answer", CallActionReceiver.ANSWER, call.id, 2))
        if (!quiet && fullScreen) builder.setFullScreenIntent(open, true)
        return builder.build()
    }

    private fun buildOngoing(call: LiveCall): Notification {
        val open = openScreen()
        val title = callTitle(call)
        return run {
            val builder = Notification.Builder(context, ONGOING_CHANNEL)
                .setSmallIcon(R.drawable.ic_call_notification)
                .setContentTitle(title)
                .setContentText(if (call.phase == CallPhase.ACTIVE) "Call in progress" else LiveCalls.status(call, 0))
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(action("Hang up", CallActionReceiver.HANG_UP, call.id, 3))
            call.connectedAtMillis?.let { builder.setWhen(it).setShowWhen(true).setUsesChronometer(true) }
            builder.build()
        }
    }

    private fun action(label: String, action: String, callId: String, requestCode: Int): Notification.Action {
        val intent = Intent(context, CallActionReceiver::class.java)
            .setAction(action)
            .putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = Icon.createWithResource(context, R.drawable.ic_call_notification)
        return Notification.Action.Builder(icon, label, pending).build()
    }

    private companion object {
        const val INCOMING_CHANNEL = "incoming_calls"
        const val INCOMING_QUIET_CHANNEL = "incoming_calls_quiet"
        const val ONGOING_CHANNEL = "ongoing_calls"
        const val NOTIFICATION_ID = 1
    }
}
