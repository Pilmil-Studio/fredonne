package fr.fredonne.core.music

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** État d'accordage par rapport à la note chromatique la plus proche. */
enum class TuningStatus {
    FLAT,
    IN_TUNE,
    SHARP,
}

/**
 * Résultat d'analyse d'un accordeur chromatique.
 *
 * @property frequencyHz Fréquence fondamentale mesurée en Hz.
 * @property nearestPitch Note chromatique la plus proche.
 * @property centsOffset Écart en cents dans `[-50.0, +50.0]` par rapport à [nearestPitch].
 * @property status État d'accordage selon le seuil configuré.
 * @property confidence Indice de confiance dans `[0.0, 1.0]`.
 */
data class TunerReading(
    val frequencyHz: Double,
    val nearestPitch: Pitch,
    val centsOffset: Double,
    val status: TuningStatus,
    val confidence: Double,
)

/**
 * Accordeur chromatique et diapason hors-ligne réutilisant [AudioPitchDetector].
 *
 * @param referenceA4Hz Fréquence de référence du La4 (440.0 Hz par défaut, 442.0 Hz en orchestre,
 * bornée à `415.0..466.0` Hz).
 * @param inTuneThresholdCents Tolérance en cents autour de 0 pour considérer la note juste.
 */
class ChromaticTuner(
    val referenceA4Hz: Double = 440.0,
    val inTuneThresholdCents: Double = 5.0,
) {
    init {
        require(referenceA4Hz in 415.0..466.0) {
            "Fréquence de référence La4 hors plage (415..466 Hz) : $referenceA4Hz"
        }
        require(inTuneThresholdCents > 0.0) {
            "Le seuil de justesse doit être positif : $inTuneThresholdCents"
        }
    }

    /** Fréquence exacte en Hz de la note [midi] selon [referenceA4Hz]. */
    fun targetFrequencyHz(midi: Int): Double =
        referenceA4Hz * 2.0.pow((midi - 69) / 12.0)

    /** Évalue une fréquence déjà détectée par rapport au diapason [referenceA4Hz]. */
    fun evaluateFrequency(frequencyHz: Double, confidence: Double = 1.0): TunerReading? {
        if (!frequencyHz.isFinite() || frequencyHz <= 0.0) return null

        val midiFloat = 69.0 + 12.0 * (ln(frequencyHz / referenceA4Hz) / LN_2)
        val nearestMidi = midiFloat.roundToInt().coerceIn(0, 127)
        val centsOffset = (midiFloat - nearestMidi) * 100.0
        val status = when {
            centsOffset < -inTuneThresholdCents -> TuningStatus.FLAT
            centsOffset > inTuneThresholdCents -> TuningStatus.SHARP
            else -> TuningStatus.IN_TUNE
        }
        val nearestPitch = AudioMelodyTranscriber.spellMidi(nearestMidi, NEUTRAL_KEY)

        return TunerReading(
            frequencyHz = frequencyHz,
            nearestPitch = nearestPitch,
            centsOffset = centsOffset,
            status = status,
            confidence = confidence.coerceIn(0.0, 1.0),
        )
    }

    /** Analyse un buffer PCM mono via [AudioPitchDetector] et retourne la lecture d'accordage. */
    fun analyzeSamples(samples: FloatArray, sampleRate: Int = 16_000): TunerReading? {
        val estimate = AudioPitchDetector(sampleRate = sampleRate).estimate(samples)
        val freq = estimate.frequencyHz ?: return null
        return evaluateFrequency(freq, estimate.confidence)
    }

    private companion object {
        val LN_2 = ln(2.0)
        val NEUTRAL_KEY = Key(Step.C, 0, Mode.MAJOR)
    }
}
