package com.example.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

object SketchAudioEngine {
    private const val SAMPLE_RATE = 22050

    private val bounceSamples: ShortArray
    private val springSamples: ShortArray
    private val scratchSamples: ShortArray
    private val crumpleSamples: ShortArray
    private val victorySamples: ShortArray

    init {
        // Pre-generate all beautiful sketchbook sound effects
        bounceSamples = generateBounce()
        springSamples = generateSpring()
        scratchSamples = generateScratch()
        crumpleSamples = generateCrumple()
        victorySamples = generateVictory()
    }

    fun playBounce() {
        playRaw(bounceSamples)
    }

    fun playSpring() {
        playRaw(springSamples)
    }

    fun playScratch() {
        playRaw(scratchSamples)
    }

    fun playCrumple() {
        playRaw(crumpleSamples)
    }

    fun playVictory() {
        playRaw(victorySamples)
    }

    private fun playRaw(samples: ShortArray) {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()

                val ms = (samples.size * 1000L) / SAMPLE_RATE
                kotlinx.coroutines.delay(ms + 100L)
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateBounce(): ShortArray {
        val duration = 0.08f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val decay = exp(-35f * t).toFloat()
            val wave = sin(2f * Math.PI.toFloat() * 540f * t)
            val noise = (Math.random().toFloat() * 2f - 1f) * exp(-140f * t).toFloat() * 0.35f
            buffer[i] = ((wave * 0.38f + noise) * decay * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
        }
        return buffer
    }

    private fun generateSpring(): ShortArray {
        val duration = 0.25f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val decay = exp(-10f * t).toFloat()
            val freq = 140f + 250f * sin(2f * Math.PI.toFloat() * 11f * t)
            val wave = sin(2f * Math.PI.toFloat() * freq * t)
            buffer[i] = (wave * decay * 0.35f * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
        }
        return buffer
    }

    private fun generateScratch(): ShortArray {
        val duration = 0.12f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(numSamples)
        var prevSample = 0f
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val env = sin(progress * Math.PI.toFloat())

            val rawNoise = (Math.random().toFloat() * 2f - 1f)
            val highPass = rawNoise - prevSample
            prevSample = rawNoise

            val grain = if (Math.random() < 0.006) (Math.random().toFloat() * 2f - 1f) * 0.35f else 0f
            val mixed = (highPass * 0.12f + grain) * env

            buffer[i] = (mixed * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
        }
        return buffer
    }

    private fun generateCrumple(): ShortArray {
        val duration = 0.65f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(numSamples)
        val crackles = FloatArray(numSamples)

        val numSnaps = 14
        val rand = java.util.Random(1337)
        val snapPositions = IntArray(numSnaps) { (rand.nextDouble() * numSamples).toInt() }
        val snapStrengths = FloatArray(numSnaps) { 0.25f + rand.nextFloat() * 0.6f }

        for (s in 0 until numSnaps) {
            val pos = snapPositions[s]
            val strength = snapStrengths[s]
            for (offset in 0 until 1400) {
                val idx = pos + offset
                if (idx < numSamples) {
                    val t = offset / 1400f
                    val decay = exp(-15f * t).toFloat()
                    val noise = (Math.random().toFloat() * 2f - 1f) * strength * decay
                    crackles[idx] += noise
                }
            }
        }

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val envelope = (1f - progress)
            val thud = sin(2f * Math.PI.toFloat() * 90f * progress) * exp(-7f * progress).toFloat()
            val mixed = (thud * 0.22f + crackles[i] + (Math.random().toFloat() * 2f - 1f) * 0.12f * envelope) * envelope
            buffer[i] = (mixed * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
        }
        return buffer
    }

    private fun generateVictory(): ShortArray {
        val duration = 1.0f
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(numSamples)
        val freqs = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        val delays = floatArrayOf(0.0f, 0.14f, 0.28f, 0.42f)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            var sum = 0f
            for (n in 0 until freqs.size) {
                val noteT = t - delays[n]
                if (noteT > 0f) {
                    val decay = exp(-4.2f * noteT).toFloat()
                    val f = freqs[n]
                    val primary = sin(1.98f * Math.PI.toFloat() * f * noteT)
                    val overtone1 = sin(2f * Math.PI.toFloat() * (f * 2.01f) * noteT) * 0.28f
                    val overtone2 = sin(2f * Math.PI.toFloat() * (f * 3.02f) * noteT) * 0.08f
                    sum += (primary + overtone1 + overtone2) * decay * 0.20f
                }
            }

            val celebrateRustle = if (t > 0.4f) {
                val rt = t - 0.4f
                val env = sin(rt * Math.PI.toFloat() / 0.6f)
                (Math.random().toFloat() * 2f - 1f) * 0.035f * env
            } else 0f

            buffer[i] = ((sum + celebrateRustle) * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
        }
        return buffer
    }
}
