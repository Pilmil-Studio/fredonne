package fr.fredonne.core.music

import kotlin.math.roundToInt

/**
 * Configuration d'une boucle A–B avec variation de tempo sans altération de hauteur.
 *
 * @property startMeasure Indice (base 0) de la première mesure de la boucle.
 * @property endMeasure Indice (base 0, inclus) de la dernière mesure de la boucle.
 * @property speedFactor Facteur de vitesse dans `0.25..2.0`.
 */
data class PracticeLoop(
    val startMeasure: Int,
    val endMeasure: Int,
    val speedFactor: Double = 1.0,
) {
    init {
        require(startMeasure >= 0) { "startMeasure doit être >= 0 : $startMeasure" }
        require(endMeasure >= startMeasure) {
            "endMeasure ($endMeasure) doit être >= startMeasure ($startMeasure)"
        }
        require(speedFactor in 0.25..2.0) {
            "speedFactor doit être dans 0.25..2.0 : $speedFactor"
        }
    }

    /** Calcule le tempo effectif en BPM (borné à `20..300`) à partir de [baseTempoBpm]. */
    fun effectiveBpm(baseTempoBpm: Int): Int =
        (baseTempoBpm * speedFactor).roundToInt().coerceIn(20, 300)

    /**
     * Extrait une nouvelle [Melody] ne contenant que les mesures `[startMeasure..endMeasure]`
     * sans modifier la partition originale ni les hauteurs des notes.
     */
    fun sliceMelody(melody: Melody): Melody {
        require(endMeasure < melody.measures.size) {
            "endMeasure ($endMeasure) hors bornes (nombre de mesures : ${melody.measures.size})"
        }
        return melody.copy(
            measures = melody.measures.subList(startMeasure, endMeasure + 1).toList(),
        )
    }

    companion object {
        val PRESET_SPEEDS: List<Double> = listOf(0.5, 0.6, 0.8, 1.0)
    }
}
