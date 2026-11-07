package com.vibra.wanderai.data.wear

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.repository.GuidanceAnnouncer
import com.vibra.wanderai.domain.repository.GuidanceTextProvider
import java.util.Locale

/**
 * The application context does not follow the per-app language chosen with
 * AppCompatDelegate on API < 33, so resources are resolved for that locale explicitly.
 */
private fun Context.appLocale(): Locale =
    AppCompatDelegate.getApplicationLocales()[0] ?: resources.configuration.locales[0] ?: Locale.getDefault()

private fun Context.localizedResources(): Resources {
    if (AppCompatDelegate.getApplicationLocales().isEmpty) return resources
    val config = Configuration(resources.configuration).apply { setLocale(appLocale()) }
    return createConfigurationContext(config).resources
}

class ResourceGuidanceTextProvider(context: Context) : GuidanceTextProvider {
    private val appContext = context.applicationContext

    override fun instruction(action: NavigationAction, distanceMeters: Int): String {
        val res = appContext.localizedResources()
        val distance = distance(distanceMeters)
        return when (action) {
            NavigationAction.TURN_LEFT -> res.getString(R.string.haptic_turn_left, distance)
            NavigationAction.TURN_RIGHT -> res.getString(R.string.haptic_turn_right, distance)
            NavigationAction.U_TURN -> res.getString(R.string.wear_instruction_u_turn, distance)
            NavigationAction.STRAIGHT -> res.getString(R.string.haptic_straight, distance)
            NavigationAction.ARRIVED -> res.getString(R.string.haptic_arrived)
            NavigationAction.ALERT -> res.getString(R.string.wear_instruction_alert)
        }
    }

    /** "850 m" / "1.4 km" — same format as the phone UI. */
    override fun distance(meters: Int): String =
        if (meters < 1_000) "$meters m" else String.format(Locale.US, "%.1f km", meters / 1_000f)
}

/** Shows a toast and, for repeated instructions, reads them aloud with TextToSpeech. */
class PhoneGuidanceAnnouncer(context: Context) : GuidanceAnnouncer, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingUtterance: String? = null

    override fun repeatInstruction(instruction: String) {
        toast(instruction)
        speak(instruction)
    }

    override fun guidancePaused() = toast(R.string.wear_guidance_paused)

    override fun guidanceResumed() = toast(R.string.wear_guidance_resumed)

    private fun speak(text: String) {
        val engine = tts ?: TextToSpeech(appContext, this).also { tts = it }
        if (!ttsReady) {
            pendingUtterance = text
            return
        }
        engine.language = appContext.appLocale()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            tts?.shutdown()
            tts = null
            pendingUtterance = null
            return
        }
        ttsReady = true
        pendingUtterance?.let(::speak)
        pendingUtterance = null
    }

    private fun toast(@StringRes res: Int) = toast(appContext.localizedResources().getString(res))

    private fun toast(text: String) = Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()

    private companion object {
        const val TAG = "VibraGuidance"
        const val UTTERANCE_ID = "wanderai_repeat_instruction"
    }
}
