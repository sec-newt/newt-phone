package io.github.secnewt.dialer.voicemail

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TelephonyNetworkSpecifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs [block] on this SIM's mobile data connection, never Wi-Fi, and lets the connection go
 * afterwards. Returns null if mobile data isn't available within [timeoutMillis].
 */
suspend fun <T> withCellularNetwork(
    context: Context,
    subId: Int,
    timeoutMillis: Long = 30_000,
    block: (Network) -> T,
): T? {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .setNetworkSpecifier(TelephonyNetworkSpecifier.Builder().setSubscriptionId(subId).build())
        .build()
    val available = CompletableDeferred<Network>()
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            available.complete(network)
        }
    }
    connectivity.requestNetwork(request, callback)
    try {
        val network = withTimeoutOrNull(timeoutMillis) { available.await() } ?: return null
        return withContext(Dispatchers.IO) { block(network) }
    } finally {
        connectivity.unregisterNetworkCallback(callback)
    }
}
