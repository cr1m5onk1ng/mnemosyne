package com.mnemosyne.library.core.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress

suspend fun Network.canPing(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val socket = socketFactory.createSocket() ?: throw IOException("Socket is null.")
            ensureActive()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            ensureActive()
            true
        } catch (e: IOException){
            false
        }
    }
}

fun NetworkCapabilities?.isInternetConnectionAvailable(): Boolean = when {
    this == null -> false
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> true
    else -> false
}

interface ConnectionAgent<out TInfo> {
    operator fun invoke(): Flow<TInfo>
}

enum class ConnectionAvailabilityState {
    UNKNOWN,
    CONNECTED,
    DISCONNECTED
}

fun ConnectivityManager.internetConnectionAvailable(network: Network): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getNetworkCapabilities(Network.fromNetworkHandle(network.networkHandle))
            .isInternetConnectionAvailable()
    } else {
        val nInfo = getNetworkInfo(network)
        nInfo != null && nInfo.isConnected
    }
}

suspend fun Context.internetConnectionAvailable() : Boolean {
    val cm = getSystemService(ConnectivityManager::class.java) as ConnectivityManager

    return cm.activeNetwork?.canPing() ?: false
}

class InternetConnectionAvailabilityAgent(
    private val connectivityManager: ConnectivityManager,
    scope: CoroutineScope
) : ConnectionAgent<ConnectionAvailabilityState>, CoroutineScope by scope {
    override fun invoke(): Flow<ConnectionAvailabilityState> {
        return callbackFlow {
            launch {
                connectivityManager.activeNetwork?.let {
                    val connectionAvailable = connectivityManager.internetConnectionAvailable(it)
                    send(
                        if(connectionAvailable) {
                            ConnectionAvailabilityState.CONNECTED
                        } else {
                            ConnectionAvailabilityState.DISCONNECTED
                        }
                    )
                } ?: send(ConnectionAvailabilityState.DISCONNECTED)
            }

            val callback = object: ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)

                    launch {
                        send(ConnectionAvailabilityState.DISCONNECTED)
                    }
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)

                    val connectivityState = if(connectivityManager.internetConnectionAvailable(network)) {
                        ConnectionAvailabilityState.CONNECTED
                    } else {
                        ConnectionAvailabilityState.DISCONNECTED
                    }

                    launch {
                        send(connectivityState)
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)

                    launch {
                        val internetAvailable = networkCapabilities.isInternetConnectionAvailable()
                        send(
                            if(internetAvailable) {
                                ConnectionAvailabilityState.CONNECTED
                            } else {
                                ConnectionAvailabilityState.DISCONNECTED
                            }
                        )
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

}