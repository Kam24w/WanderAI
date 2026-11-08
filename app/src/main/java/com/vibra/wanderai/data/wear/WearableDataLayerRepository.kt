package com.vibra.wanderai.data.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * [WearDataLayerRepository] backed by Google Play services Wearable (see [WearProtocol]).
 *
 * Watch actions arrive through [WatchActionListenerService], which the system
 * starts even when WanderAI is closed, and are forwarded to [onWatchMessage].
 * (No runtime MessageClient listener: it would deliver every action twice while
 * the app is open.) Every call fails gracefully (logged, [Result.failure]) on
 * phones without the Wear OS companion.
 *
 * [devBridge] is only non-null in debug builds (see
 * [com.vibra.wanderai.core.DevEnvironment]): when Play services reports no nodes or
 * fails (e.g. "Got 0 accounts"), the watch emulator is reached via [LocalDevBridge]
 * over the ADB tunnel instead. In release builds it is null and nothing changes.
 */
class WearableDataLayerRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val devBridge: LocalDevBridge? = null,
) : WearDataLayerRepository {

    private val appContext = context.applicationContext
    private val messageClient = Wearable.getMessageClient(appContext)
    private val dataClient = Wearable.getDataClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)

    private val _connectedWatches = MutableStateFlow<List<WearWatch>>(emptyList())
    override val connectedWatches: StateFlow<List<WearWatch>> = _connectedWatches.asStateFlow()

    private val _userActions = MutableSharedFlow<WatchUserAction>(extraBufferCapacity = 16)
    override val userActions: Flow<WatchUserAction> = _userActions.asSharedFlow()

    init {
        devBridge?.let { bridge ->
            bridge.start(scope)
            scope.launch { bridge.incoming.collect { onWatchMessage(it.path, it.data) } }
            scope.launch { bridge.connected.collect { refreshConnectedWatches() } }
        }

        // Poll reachable watches only while some screen observes them.
        scope.launch {
            _connectedWatches.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { observed ->
                    while (observed) {
                        refreshConnectedWatches()
                        delay(NODE_POLL_MS)
                    }
                }
        }
    }

    override fun onWatchMessage(path: String, data: ByteArray) {
        if (path != WearProtocol.PATH_USER_ACTION) return
        val action = WearProtocol.decodeUserAction(data)
        if (action == null) {
            Log.w(TAG, "Ignoring unknown watch action: ${data.toString(Charsets.UTF_8)}")
            return
        }
        _userActions.tryEmit(action)
    }

    override suspend fun sendNavigationEvent(event: NavigationEvent): Result<Int> = runCatching {
        val watches = refreshConnectedWatches()
        check(watches.isNotEmpty()) { VibraError.NOT_CONNECTED.name }

        val payload = WearProtocol.encodeNavigationEvent(event)
        val delivered = coroutineScope {
            watches.map { watch ->
                async {
                    runCatching {
                        if (watch.id == LocalDevBridge.NODE_ID) {
                            check(devBridge?.send(WearProtocol.PATH_NAVIGATION_EVENT, payload) == true)
                        } else {
                            messageClient.sendMessage(watch.id, WearProtocol.PATH_NAVIGATION_EVENT, payload).await()
                        }
                    }.onFailure { Log.w(TAG, "sendMessage to ${watch.displayName} failed", it) }.isSuccess
                }
            }.awaitAll()
        }.count { it }

        check(delivered > 0) { VibraError.WRITE_FAILED.name }
        delivered
    }

    override suspend fun updateActiveRoute(route: ActiveRouteState): Result<Unit> {
        val bridge = devBridge ?: return putActiveRoute(route)
        // Emulator: mirror over the bridge too (it replays the last route on connect).
        val bridged = bridge.send(
            WearProtocol.PATH_ACTIVE_ROUTE,
            LocalDevBridge.encodeActiveRoute(route.currentDestination, route.remainingDistance, route.routeActive),
        )
        val dataLayer = putActiveRoute(route)
        return if (bridged) Result.success(Unit) else dataLayer
    }

    private suspend fun putActiveRoute(route: ActiveRouteState): Result<Unit> = runCatching {
        val request = PutDataMapRequest.create(WearProtocol.PATH_ACTIVE_ROUTE).apply {
            dataMap.putString(WearProtocol.KEY_CURRENT_DESTINATION, route.currentDestination)
            dataMap.putString(WearProtocol.KEY_REMAINING_DISTANCE, route.remainingDistance)
            dataMap.putBoolean(WearProtocol.KEY_ROUTE_ACTIVE, route.routeActive)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
        Unit
    }.onFailure { Log.w(TAG, "putDataItem(active_route) failed", it) }

    override suspend fun refreshConnectedWatches(): List<WearWatch> {
        val watches = runCatching { nodeClient.connectedNodes.await() }
            .onFailure { Log.d(TAG, "connectedNodes unavailable", it) }
            .getOrDefault(emptyList())
            .map { WearWatch(id = it.id, displayName = it.displayName) }
            .ifEmpty {
                // Emulator without Play services pairing: fall back to the ADB bridge.
                if (devBridge?.connected?.value == true) listOf(BRIDGE_WATCH) else emptyList()
            }
        _connectedWatches.value = watches
        return watches
    }

    private companion object {
        const val TAG = "VibraWear"
        const val NODE_POLL_MS = 15_000L
        val BRIDGE_WATCH = WearWatch(id = LocalDevBridge.NODE_ID, displayName = "Vibra (puente ADB)")
    }
}
