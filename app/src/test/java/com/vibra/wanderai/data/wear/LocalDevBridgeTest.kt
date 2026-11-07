package com.vibra.wanderai.data.wear

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class LocalDevBridgeTest {

    @Test
    fun `frame round trip keeps path and payload`() {
        val bytes = ByteArrayOutputStream()
        val payload = "{\"action\":\"ACK\"}".toByteArray()
        LocalDevBridge.writeFrame(DataOutputStream(bytes), WearProtocol.PATH_USER_ACTION, payload)

        val frame = LocalDevBridge.readFrame(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))

        assertEquals(WearProtocol.PATH_USER_ACTION, frame.path)
        assertArrayEquals(payload, frame.data)
    }

    @Test(expected = IOException::class)
    fun `oversized frame is rejected`() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply { writeUTF("/x"); writeInt(10_000_000) }
        LocalDevBridge.readFrame(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
    }

    @Test
    fun `active route encoding matches the watch format`() {
        val encoded = LocalDevBridge.encodeActiveRoute("Catedral", "1.4 km", true).toString(Charsets.UTF_8)
        assertEquals("true\u001FCatedral\u001F1.4 km", encoded)
    }
}
