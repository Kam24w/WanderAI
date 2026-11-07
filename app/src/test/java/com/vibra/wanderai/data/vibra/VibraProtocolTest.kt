package com.vibra.wanderai.data.vibra

import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.TurnDirection
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VibraProtocolTest {

    @Test
    fun `turn right in 70 m encodes opcode, intensity and big-endian metres`() {
        val frame = VibraProtocol.encode(HapticCommand.Turn(TurnDirection.RIGHT, 70), HapticIntensity.STRONG)
        assertArrayEquals(byteArrayOf(0x02, 0x03, 0x00, 70), frame)
    }

    @Test
    fun `large distances are split across two bytes and clamped`() {
        val frame = VibraProtocol.encode(HapticCommand.Straight(1_000), HapticIntensity.SOFT)
        assertArrayEquals(byteArrayOf(0x03, 0x01, 0x03, 0xE8.toByte()), frame)

        val clamped = VibraProtocol.encode(HapticCommand.Straight(200_000), HapticIntensity.SOFT)
        assertEquals(0xFF.toByte(), clamped[2])
        assertEquals(0xFF.toByte(), clamped[3])
    }

    @Test
    fun `sync header carries the stop count`() {
        val frame = VibraProtocol.encode(HapticCommand.ItinerarySync(stopCount = 4, firstStopMeters = 300), HapticIntensity.MEDIUM)
        assertArrayEquals(byteArrayOf(0x10, 0x02, 0x00, 0x04), frame)
    }

    @Test
    fun `left and right patterns differ in pulse count`() {
        val (left, _) = VibraProtocol.waveform(HapticCommand.Turn(TurnDirection.LEFT, 10), HapticIntensity.MEDIUM)
        val (right, _) = VibraProtocol.waveform(HapticCommand.Turn(TurnDirection.RIGHT, 10), HapticIntensity.MEDIUM)
        assertEquals(2, left.size / 2)
        assertEquals(3, right.size / 2)
    }
}
