package com.vibra.wanderai.core

import android.os.Build
import com.vibra.wanderai.BuildConfig

/** Detección del entorno de desarrollo local (emulador). */
object DevEnvironment {

    val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.startsWith("generic") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
    }

    /**
     * Servidor del puente ADB: en cualquier build debug, emulador o teléfono físico,
     * porque el escenario habitual es teléfono físico por USB + reloj emulado
     * (`adb -s <teléfono> forward tcp:5650 tcp:5650`). Solo escucha en loopback (no es
     * accesible por Wi-Fi) y solo se usa si la Data Layer no devuelve nodos, así que con
     * un reloj real emparejado no cambia nada. En release siempre es false.
     */
    val localBridgeEnabled: Boolean get() = BuildConfig.DEBUG
}
