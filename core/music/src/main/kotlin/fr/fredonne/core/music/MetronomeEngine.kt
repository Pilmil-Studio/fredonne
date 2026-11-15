package fr.fredonne.core.music

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Battement généré par le métronome.
 *
 * @property beatIndex Indice global du battement (base 0).
 * @property measureBeat Temps dans la mesure (base 1).
 * @property isAccent Vrai sur le temps fort (temps 1 de chaque mesure).
 * @property timestampMs Horodatage théorique en millisecondes depuis le début.
 */
data class MetronomeBeat(
    val beatIndex: Int,
    val measureBeat: Int,
    val isAccent: Boolean,
    val timestampMs: Long,
)

/**
 * Moteur de métronome et de décompte avant capture micro, 100 % hors-ligne.
 */
object MetronomeEngine {

    const val MIN_BPM = 30
    const val MAX_BPM = 240

    /** Durée d'un battement en millisecondes pour [bpm] (borné à `30..240`). */
    fun beatIntervalMs(bpm: Int): Double =
        60_000.0 / bpm.coerceIn(MIN_BPM, MAX_BPM)

    /**
     * Génère la grille de battements pour [barCount] mesures en [timeSignature] à [bpm].
     *
     * Pour les mesures en `6/8`, [compoundSixEightByDottedQuarter] permet de battre à la noire
     * pointée (2 temps par mesure) plutôt qu'à la croche (6 temps par mesure).
     */
    fun generateBeats(
        timeSignature: TimeSignature,
        bpm: Int,
        barCount: Int,
        compoundSixEightByDottedQuarter: Boolean = false,
    ): List<MetronomeBeat> {
        require(barCount >= 0) { "barCount doit être >= 0 : $barCount" }
        if (barCount == 0) return emptyList()

        val beatsPerBar = if (
            compoundSixEightByDottedQuarter &&
            timeSignature.beats == 6 &&
            timeSignature.unit == 8
        ) {
            2
        } else {
            timeSignature.beats
        }

        val intervalMs = beatIntervalMs(bpm)
        val totalBeats = barCount * beatsPerBar

        return List(totalBeats) { index ->
            val measureBeat = (index % beatsPerBar) + 1
            MetronomeBeat(
                beatIndex = index,
                measureBeat = measureBeat,
                isAccent = measureBeat == 1,
                timestampMs = (index * intervalMs).roundToLong(),
            )
        }
    }

    /** Génère un décompte de [bars] mesure(s) avant le début d'une capture micro. */
    fun countInBeats(
        timeSignature: TimeSignature,
        bpm: Int,
        bars: Int = 1,
    ): List<MetronomeBeat> = generateBeats(
        timeSignature = timeSignature,
        bpm = bpm,
        barCount = bars,
    )
}

/**
 * Suivi du tempo par frappe manuelle (Tap Tempo) avec moyenne glissante et remise à zéro
 * automatique après inactivité.
 */
class TapTempoTracker(
    private val maxTaps: Int = 8,
    private val resetTimeoutMs: Long = 2_500L,
) {
    init {
        require(maxTaps >= 2) { "maxTaps doit être >= 2 : $maxTaps" }
        require(resetTimeoutMs > 0L) { "resetTimeoutMs doit être > 0 : $resetTimeoutMs" }
    }

    private val timestamps = ArrayDeque<Long>()

    /** Enregistre un tap à [nowMs] et retourne le BPM estimé, ou `null` s'il n'y a qu'un tap. */
    fun tap(nowMs: Long): Int? {
        if (timestamps.isNotEmpty()) {
            val last = timestamps.last()
            if (nowMs < last || nowMs - last > resetTimeoutMs) {
                timestamps.clear()
            }
        }
        timestamps.addLast(nowMs)
        while (timestamps.size > maxTaps) {
            timestamps.removeFirst()
        }
        return currentBpm()
    }

    /** Retourne le BPM courant sur la fenêtre glissante, borné à `30..240`, ou `null`. */
    fun currentBpm(): Int? {
        if (timestamps.size < 2) return null
        val spanMs = timestamps.last() - timestamps.first()
        if (spanMs <= 0L) return null
        val avgIntervalMs = spanMs.toDouble() / (timestamps.size - 1)
        return (60_000.0 / avgIntervalMs)
            .roundToInt()
            .coerceIn(MetronomeEngine.MIN_BPM, MetronomeEngine.MAX_BPM)
    }

    /** Réinitialise l'historique des frappes. */
    fun reset() {
        timestamps.clear()
    }
}
