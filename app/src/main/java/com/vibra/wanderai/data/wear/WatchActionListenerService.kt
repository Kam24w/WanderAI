package com.vibra.wanderai.data.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.vibra.wanderai.WanderAIApplication

/**
 * Receives `/vibra/user_action` from the Vibra watch even when WanderAI is closed or
 * in the background: Play services starts the process (and with it
 * [WanderAIApplication], which starts the guidance controller) only for that path
 * (see the manifest filter).
 *
 * The message is handed to the repository, which decodes it with [WearProtocol] and
 * emits it on `userActions` for [com.vibra.wanderai.domain.usecase.NavigationGuidanceController].
 */
class WatchActionListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearProtocol.PATH_USER_ACTION) return
        val container = (application as WanderAIApplication).container
        container.wearDataLayerRepository.onWatchMessage(messageEvent.path, messageEvent.data)
    }
}
