package com.vibra.wanderai.data.wear

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Puente de desarrollo (solo builds debug) que sustituye a la Data Layer cuando
 * Google Play Services del reloj emulado no puede emparejar (p. ej. "Got 0 accounts" o consentimiento
 * global pendiente en el emulador).
 *
 * El teléfono (físico por USB o emulado) escucha en `127.0.0.1:[PORT]`; con
 * `adb -s <serial-del-TELEFONO> forward tcp:5650 tcp:5650`
 * el reloj emulado llega a él en `10.0.2.2:5650` (el host). adbd entrega el tráfico
 * por loopback, así que no hace falta escuchar en 0.0.0.0 (y en un teléfono físico
 * eso expondría el puerto en la Wi-Fi). Lleva exactamente los
 * mismos paths y payloads que [WearProtocol]; ver [Frame] para el formato.
 *
 * Nunca se instancia en builds release (ver [com.vibra.wanderai.core.DevEnvironment]).
 */
class LocalDevBridge(private val port: Int = PORT) {

    /** Un mensaje del puente: `path` + payload, igual que un MessageEvent. */
    class Frame(val path: String, val data: ByteArray)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _incoming = MutableSharedFlow<Frame>(extraBufferCapacity = 16)
    val incoming: SharedFlow<Frame> = _incoming.asSharedFlow()

    private val writeMutex = Mutex()
    @Volatile private var client: Socket? = null
    @Volatile private var output: DataOutputStream? = null

    /** Último estado de ruta: se reenvía al conectar, como hace un DataItem persistente. */
    @Volatile private var lastRoute: Frame? = null

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching { serve() }
                    .onFailure { Log.w(TAG, "Puente local caído, reintentando", it) }
                delay(RESTART_DELAY_MS)
            }
        }
    }

    private suspend fun serve() {
        ServerSocket(port, 1, InetAddress.getLoopbackAddress()).use { server ->
            Log.i(TAG, "Puente de desarrollo escuchando en 127.0.0.1:$port (adb forward tcp:$port tcp:$port)")
            while (true) {
                val socket = server.accept()
                handleClient(socket)
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.tcpNoDelay = true
        val out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        writeMutex.withLock {
            client = socket
            output = out
        }
        _connected.value = true
        Log.i(TAG, "Reloj conectado por el puente local")
        lastRoute?.let { send(it.path, it.data) }
        try {
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            while (true) {
                _incoming.emit(readFrame(input))
            }
        } catch (_: EOFException) {
            // Cierre ordenado del reloj.
        } catch (e: IOException) {
            Log.d(TAG, "Conexión del puente cerrada: ${e.message}")
        } finally {
            writeMutex.withLock {
                if (client === socket) {
                    client = null
                    output = null
                }
            }
            runCatching { socket.close() }
            _connected.value = false
            Log.i(TAG, "Reloj desconectado del puente local")
        }
    }

    /** Envía un frame al reloj; false si no hay ninguno conectado o la escritura falla. */
    suspend fun send(path: String, data: ByteArray): Boolean {
        if (path == WearProtocol.PATH_ACTIVE_ROUTE) lastRoute = Frame(path, data)
        return withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val out = output ?: return@withLock false
                runCatching { writeFrame(out, path, data) }
                    .onFailure {
                        Log.w(TAG, "Escritura en el puente fallida", it)
                        runCatching { client?.close() }
                    }
                    .isSuccess
            }
        }
    }

    companion object {
        const val PORT = 5650
        const val NODE_ID = "local-dev-bridge"
        private const val TAG = "VibraDevBridge"
        private const val RESTART_DELAY_MS = 3_000L
        private const val MAX_FRAME_BYTES = 64 * 1024

        /** Formato: `writeUTF(path)` + `writeInt(longitud)` + bytes. */
        fun writeFrame(out: DataOutputStream, path: String, data: ByteArray) {
            out.writeUTF(path)
            out.writeInt(data.size)
            out.write(data)
            out.flush()
        }

        fun readFrame(input: DataInputStream): Frame {
            val path = input.readUTF()
            val size = input.readInt()
            if (size !in 0..MAX_FRAME_BYTES) throw IOException("Frame inválido: $size bytes")
            val data = ByteArray(size).also { input.readFully(it) }
            return Frame(path, data)
        }

        /** Ruta activa serializada para el puente (en la Data Layer real va en un DataMap). */
        fun encodeActiveRoute(destination: String, remainingDistance: String, routeActive: Boolean): ByteArray =
            listOf(routeActive.toString(), destination, remainingDistance)
                .joinToString(FIELD_SEPARATOR.toString())
                .toByteArray(Charsets.UTF_8)

        const val FIELD_SEPARATOR = '\u001F'
    }
}
