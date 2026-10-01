package com.olegovichhh.audioanonymizer

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object WavDsp {
    data class Wav(val sampleRate: Int, val channels: Int, val samples: ShortArray)

    fun process(w: Wav, semitones: Float, tempo: Float, grainMs: Int, jitter: Float): Wav {
        var samples = w.samples
        if (abs(tempo - 1f) > 0.01f) samples = resample(samples, w.channels, tempo)
        if (abs(semitones) > 0.01f) {
            val ratio = 2.0.pow(semitones / 12.0).toFloat()
            samples = resample(samples, w.channels, ratio)
        }
        if (grainMs > 0 && jitter > 0f) {
            val grainSamples = max(w.channels, (w.sampleRate * grainMs / 1000) * w.channels)
            samples = granular(samples, w.channels, grainSamples, jitter)
        }
        return Wav(w.sampleRate, w.channels, samples)
    }

    private fun resample(input: ShortArray, channels: Int, ratio: Float): ShortArray {
        val frames = input.size / channels
        val outFrames = max(1, (frames / ratio).roundToInt())
        require(outFrames.toLong() * channels < Int.MAX_VALUE) { "Файл слишком большой" }
        val output = ShortArray(outFrames * channels)
        for (frame in 0 until outFrames) {
            val pos = frame * ratio
            val i = min(frames - 1, pos.toInt())
            val j = min(frames - 1, i + 1)
            val t = pos - i
            for (channel in 0 until channels) {
                val value = (1f - t) * input[i * channels + channel] + t * input[j * channels + channel]
                output[frame * channels + channel] = value.roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return output
    }

    private fun granular(input: ShortArray, channels: Int, grain: Int, jitter: Float): ShortArray {
        val grainSize = max(channels, grain - grain % channels)
        if (input.size < grainSize * 2) return input
        val output = input.copyOf()
        val count = input.size / grainSize
        for (index in 0 until count) {
            if (Random.nextFloat() < jitter) {
                val sourceIndex = (index + Random.nextInt(-2, 3)).coerceIn(0, count - 1)
                val source = sourceIndex * grainSize
                val destination = index * grainSize
                val length = min(grainSize, min(input.size - source, output.size - destination))
                input.copyInto(output, destination, source, source + length)
            }
        }
        return output
    }

    fun write(w: Wav, outputStream: OutputStream) {
        val out = DataOutputStream(BufferedOutputStream(outputStream))
        fun ascii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
        fun le16(value: Int) {
            out.writeByte(value)
            out.writeByte(value shr 8)
        }
        fun le32(value: Int) {
            out.writeByte(value)
            out.writeByte(value shr 8)
            out.writeByte(value shr 16)
            out.writeByte(value shr 24)
        }

        val dataBytes = w.samples.size * 2
        ascii("RIFF"); le32(36 + dataBytes); ascii("WAVE")
        ascii("fmt "); le32(16); le16(1); le16(w.channels)
        le32(w.sampleRate); le32(w.sampleRate * w.channels * 2)
        le16(w.channels * 2); le16(16)
        ascii("data"); le32(dataBytes)
        for (sample in w.samples) le16(sample.toInt())
        out.flush()
    }
}
