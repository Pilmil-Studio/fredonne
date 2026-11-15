package fr.fredonne.core.music

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Estimation instantanée de la hauteur fondamentale sur une fenêtre audio mono.
 */
data class PitchEstimate(
    val frequencyHz: Double?,
    val midiFloat: Double?,
    val nearestMidi: Int?,
    val centsOffset: Int,
    val confidence: Double,
    val rms: Double,
) {
    val isVoiced: Boolean get() = frequencyHz != null && nearestMidi != null
}

/**
 * Détecteur de hauteur fondamentale `f0` par fonction de différence moyenne cumulée normalisée (YIN,
 * De Cheveigné & Kawahara 2002) avec interpolation parabolique.
 *
 * Conçu pour le chant, le sifflet et les instruments monodiques entre [minFreqHz] (~C2 = 65 Hz)
 * et [maxFreqHz] (~C7 = 2093 Hz), 100 % hors-ligne en Kotlin pur.
 */
class AudioPitchDetector(
    val sampleRate: Int = 16_000,
    val minFreqHz: Double = 65.0,
    val maxFreqHz: Double = 2_093.0,
    val yinThreshold: Double = 0.15,
    val minRms: Double = 0.008,
) {
    fun estimate(frame: FloatArray): PitchEstimate {
        if (frame.size < 64) {
            return PitchEstimate(null, null, null, 0, 0.0, 0.0)
        }

        var sumSq = 0.0
        for (s in frame) sumSq += s * s
        val rms = sqrt(sumSq / frame.size)
        if (rms < minRms) {
            return PitchEstimate(null, null, null, 0, 0.0, rms)
        }

        val maxTau = minOf(frame.size / 2, (sampleRate / minFreqHz).toInt())
        val minTau = maxOf(2, (sampleRate / maxFreqHz).toInt())
        if (maxTau <= minTau + 2) {
            return PitchEstimate(null, null, null, 0, 0.0, rms)
        }

        val diff = DoubleArray(maxTau + 1)
        val halfLen = frame.size - maxTau
        for (tau in 1..maxTau) {
            var acc = 0.0
            for (j in 0 until halfLen) {
                val d = frame[j] - frame[j + tau]
                acc += d * d
            }
            diff[tau] = acc
        }

        // Fonction de différence moyenne cumulée normalisée (CMNDF)
        val cmndf = DoubleArray(maxTau + 1)
        cmndf[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..maxTau) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum == 0.0) 1.0 else diff[tau] * tau / runningSum
        }

        // Premier minimum local sous le seuil YIN
        var tauEstimate = -1
        var tau = minTau
        while (tau <= maxTau) {
            if (cmndf[tau] < yinThreshold) {
                while (tau + 1 <= maxTau && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        // Si aucun creux sous le seuil, on n'accepte pas une estimation peu fiable
        if (tauEstimate == -1) {
            var bestTau = minTau
            for (t in (minTau + 1)..maxTau) {
                if (cmndf[t] < cmndf[bestTau]) bestTau = t
            }
            val conf = (1.0 - cmndf[bestTau]).coerceIn(0.0, 1.0)
            return PitchEstimate(null, null, null, 0, conf, rms)
        }

        // Interpolation parabolique autour de tauEstimate pour une précision < 1 cent
        val betterTau = if (tauEstimate > minTau && tauEstimate < maxTau) {
            val s0 = cmndf[tauEstimate - 1]
            val s1 = cmndf[tauEstimate]
            val s2 = cmndf[tauEstimate + 1]
            val denom = 2.0 * (2.0 * s1 - s2 - s0)
            if (denom != 0.0) {
                val shift = (s2 - s0) / denom
                tauEstimate + shift.coerceIn(-0.5, 0.5)
            } else {
                tauEstimate.toDouble()
            }
        } else {
            tauEstimate.toDouble()
        }

        val freq = sampleRate / betterTau
        if (freq < minFreqHz || freq > maxFreqHz) {
            return PitchEstimate(null, null, null, 0, 0.0, rms)
        }

        val midiFloat = 69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))
        val nearestMidi = midiFloat.roundToInt().coerceIn(24, 108)
        val cents = ((midiFloat - nearestMidi) * 100.0).roundToInt()
        val confidence = (1.0 - cmndf[tauEstimate]).coerceIn(0.0, 1.0)

        return PitchEstimate(
            frequencyHz = freq,
            midiFloat = midiFloat,
            nearestMidi = nearestMidi,
            centsOffset = cents,
            confidence = confidence,
            rms = rms,
        )
    }
}
