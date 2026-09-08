package org.openbabyphone

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeWaveformTest {

    @Test
    fun `rollingLoudness returns zero for empty history`() {
        assertEquals(0f, rollingLoudness(floatArrayOf(), 1.0f), 0.001f)
    }

    @Test
    fun `rollingLoudness handles single sample without crash`() {
        val result = rollingLoudness(floatArrayOf(0.5f), 1.0f)
        assertEquals(0.5f, result, 0.001f)
    }

    @Test
    fun `waveformSamples returns requested empty shape`() {
        assertEquals(5, waveformSamples(floatArrayOf(), 1.0f, 5).size)
        waveformSamples(floatArrayOf(), 1.0f, 5).forEach {
            assertEquals(0f, it, 0.001f)
        }
    }

    @Test
    fun `waveformSamples aligns a short history to the right`() {
        val result = waveformSamples(floatArrayOf(0.7f), 1.0f, 4)

        assertEquals(0f, result[0], 0.001f)
        assertEquals(0f, result[1], 0.001f)
        assertEquals(0f, result[2], 0.001f)
        assertEquals(kotlin.math.sqrt(0.7f), result[3], 0.001f)
    }

    @Test
    fun `waveformSamples keeps only the newest fixed window`() {
        val result = waveformSamples(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), 1.0f, 2, windowSize = 2)

        assertEquals(kotlin.math.sqrt(0.3f), result[0], 0.001f)
        assertEquals(kotlin.math.sqrt(0.4f), result[1], 0.001f)
    }

    @Test
    fun `waveformSamples combines bucket average and peak`() {
        val result = waveformSamples(floatArrayOf(0.04f, 0.36f), 1.0f, 1, windowSize = 2)

        val expectedPower = 0.2f * 0.7f + 0.36f * 0.3f
        assertEquals(kotlin.math.sqrt(expectedPower), result[0], 0.001f)
    }

    @Test
    fun `waveformSamples clamps normalized power`() {
        val result = waveformSamples(floatArrayOf(0.8f), 2.0f, 1)

        assertEquals(1f, result[0], 0.001f)
    }

    @Test
    fun `audioSignalState returns no recent audio when history is empty`() {
        assertEquals(AudioSignalState.NoRecentAudio, audioSignalState(floatArrayOf(), 1.0f, 0L, 1000L))
    }

    @Test
    fun `audioSignalState returns no recent audio when frames are stale`() {
        assertEquals(AudioSignalState.NoRecentAudio, audioSignalState(floatArrayOf(0.3f), 1.0f, 1000L, 4000L))
    }

    @Test
    fun `audioSignalState returns quiet for fresh silent frames`() {
        assertEquals(AudioSignalState.Quiet, audioSignalState(floatArrayOf(0f, 0f), 1.0f, 1000L, 2000L))
    }

    @Test
    fun `audioSignalState returns sound detected for recent sound`() {
        assertEquals(
            AudioSignalState.SoundDetected,
            audioSignalState(floatArrayOf(0.1f, 0.2f), 1.0f, 1000L, 2000L)
        )
    }

    @Test
    fun `audioSignalState returns loud sound for recent loud frames`() {
        assertEquals(AudioSignalState.LoudSound, audioSignalState(floatArrayOf(0.8f, 0.9f), 1.0f, 1000L, 2000L))
    }

    @Test
    fun `audioSignalBackgroundMix scales loudness and clamps`() {
        assertEquals(
            audioSignalBackgroundMix(AudioSignalState.NoRecentAudio, 1f),
            audioSignalBackgroundMix(AudioSignalState.NoRecentAudio, 0f),
            0.001f
        )
        assertEquals(
            audioSignalBackgroundMix(AudioSignalState.Quiet, 0.56f),
            audioSignalBackgroundMix(AudioSignalState.LoudSound, 0.56f),
            0.001f
        )
        assertEquals(
            audioSignalBackgroundMix(AudioSignalState.LoudSound, 2f),
            audioSignalBackgroundMix(AudioSignalState.LoudSound, 1f),
            0.001f
        )
    }
}
