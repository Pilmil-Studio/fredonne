package fr.fredonne.app

import fr.fredonne.core.music.Event
import fr.fredonne.core.music.Fraction
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Rest
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TimeSignature
import fr.fredonne.core.music.Tuplet

/**
 * Document versionné de reprise : tout ce que porte l'[EditorState] hors historique d'annulation.
 *
 * Format texte, une ligne par enregistrement, champs séparés par une tabulation :
 * ```
 * fredonne-session<TAB>1
 * key<TAB>C<TAB>0<TAB>M          (tonique, altération, M ou m)
 * time<TAB>4<TAB>4
 * tempo<TAB>96
 * instrument<TAB>clarinet_bb     (ou « - »)
 * duration<TAB>1<TAB>4<TAB>0     (valeur choisie en fraction de ronde, pointée 0/1)
 * cursor<TAB>mesure<TAB>indice
 * loop<TAB>0<TAB>début<TAB>fin
 * speed<TAB>1.0
 * tuner<TAB>440.0
 * measure
 * N<TAB>pas<TAB>octave<TAB>altération<TAB>num<TAB>den<TAB>tupletA<TAB>tupletN<TAB>souffle<TAB>paroles
 * R<TAB>num<TAB>den<TAB>tupletA<TAB>tupletN
 * ```
 * Les lignes inconnues sont ignorées : un champ ajouté plus tard ne casse pas la lecture d'une
 * version 1. Toute autre anomalie lève une exception, que [SessionRepository] transforme en mise
 * de côté du fichier. Les formats de partage (FR1, ABC, MusicXML) ne conviennent pas ici : ils
 * perdent les paroles et les respirations, bornent le tempo et ne portent ni curseur ni boucle.
 */
object EditorSessionCodec {

    const val VERSION = 1
    private const val HEADER = "fredonne-session"

    fun encode(state: EditorState): String {
        val sb = StringBuilder()
        fun line(vararg fields: Any?) {
            sb.append(fields.joinToString("\t")).append('\n')
        }
        val melody = state.melody
        line(HEADER, VERSION)
        line("key", melody.key.tonicStep.name, melody.key.tonicAlter, if (melody.key.mode == Mode.MAJOR) "M" else "m")
        line("time", melody.time.beats, melody.time.unit)
        line("tempo", state.tempoBpm)
        line("instrument", state.instrument?.id ?: "-")
        line("duration", state.selectedDuration.numerator, state.selectedDuration.denominator, if (state.dotted) 1 else 0)
        line("cursor", state.cursorMeasure, state.cursorEventIndex)
        line("loop", if (state.loopEnabled) 1 else 0, state.loopStartMeasure, state.loopEndMeasure)
        line("speed", state.playbackSpeed)
        line("tuner", state.tunerReferenceA4Hz)
        for (measure in melody.measures) {
            sb.append("measure\n")
            for (event in measure.events) {
                val tuplet = event.tuplet
                val tupA = tuplet?.actual ?: 0
                val tupN = tuplet?.normal ?: 0
                when (event) {
                    is Note -> line(
                        "N", event.pitch.step.name, event.pitch.octave, event.pitch.alter,
                        event.written.numerator, event.written.denominator, tupA, tupN,
                        if (event.breathMark) 1 else 0, escape(event.lyric.orEmpty()),
                    )
                    is Rest -> line("R", event.written.numerator, event.written.denominator, tupA, tupN)
                }
            }
        }
        return sb.toString()
    }

    /** @throws IllegalArgumentException si le document est absent, d'une autre version ou incohérent. */
    fun decode(text: String): EditorState {
        val lines = text.split('\n').map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
        val head = lines.firstOrNull()?.split('\t')
        require(head != null && head[0] == HEADER) { "En-tête de session absent" }
        require(head.getOrNull(1)?.toIntOrNull() == VERSION) { "Version de session non prise en charge" }

        var key: Key? = null
        var time: TimeSignature? = null
        var state = EditorState()
        val measures = mutableListOf<MutableList<Event>>()

        for (raw in lines.drop(1)) {
            val f = raw.split('\t')
            when (f[0]) {
                "key" -> key = Key(
                    Step.valueOf(f[1]),
                    f[2].toInt().also { require(it in -2..2) { "Altération de tonique invalide" } },
                    when (f[3]) {
                        "M" -> Mode.MAJOR
                        "m" -> Mode.MINOR
                        else -> throw IllegalArgumentException("Mode inconnu : ${f[3]}")
                    },
                )
                "time" -> time = TimeSignature(f[1].toInt(), f[2].toInt())
                "tempo" -> state = state.copy(tempoBpm = f[1].toInt().coerceIn(20, 400))
                "instrument" -> state = state.copy(instrument = if (f[1] == "-") null else Instruments.byId(f[1]))
                "duration" -> state = state.copy(
                    selectedDuration = fraction(f[1], f[2]),
                    dotted = f[3] == "1",
                )
                "cursor" -> state = state.copy(cursorMeasure = f[1].toInt(), cursorEventIndex = f[2].toInt())
                "loop" -> state = state.copy(
                    loopEnabled = f[1] == "1",
                    loopStartMeasure = f[2].toInt(),
                    loopEndMeasure = f[3].toInt(),
                )
                "speed" -> state = state.copy(playbackSpeed = f[1].toDouble().coerceIn(0.25, 2.0))
                "tuner" -> state = state.copy(tunerReferenceA4Hz = f[1].toDouble().coerceIn(380.0, 500.0))
                "measure" -> measures.add(mutableListOf())
                "N" -> {
                    val target = requireNotNull(measures.lastOrNull()) { "Note hors mesure" }
                    target += Note(
                        pitch = Pitch(Step.valueOf(f[1]), f[2].toInt(), f[3].toInt()),
                        written = fraction(f[4], f[5]),
                        tuplet = tuplet(f[6], f[7]),
                        lyric = unescape(f.getOrElse(9) { "" }).ifEmpty { null },
                        breathMark = f[8] == "1",
                    )
                }
                "R" -> {
                    val target = requireNotNull(measures.lastOrNull()) { "Silence hors mesure" }
                    target += Rest(fraction(f[1], f[2]), tuplet(f[3], f[4]))
                }
            }
        }

        val finalKey = requireNotNull(key) { "Tonalité absente" }
        val finalTime = requireNotNull(time) { "Chiffrage absent" }
        val finalMeasures = measures.map { Measure(it.toList()) }.ifEmpty { listOf(Measure(emptyList())) }
        val lastMeasure = finalMeasures.lastIndex
        val cursorMeasure = state.cursorMeasure.coerceIn(0, lastMeasure)
        return state.copy(
            melody = Melody(finalKey, finalTime, finalMeasures),
            cursorMeasure = cursorMeasure,
            cursorEventIndex = state.cursorEventIndex.coerceIn(0, finalMeasures[cursorMeasure].events.size),
            loopStartMeasure = state.loopStartMeasure.coerceIn(0, lastMeasure),
            loopEndMeasure = state.loopEndMeasure.coerceIn(0, lastMeasure),
        )
    }

    private fun fraction(num: String, den: String): Fraction {
        val n = num.toLong()
        val d = den.toLong()
        require(n > 0 && d > 0) { "Durée invalide : $num/$den" }
        return Fraction.of(n, d)
    }

    private fun tuplet(actual: String, normal: String): Tuplet? {
        val a = actual.toInt()
        val n = normal.toInt()
        return if (a == 0 && n == 0) null else Tuplet(a, n)
    }

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(c)
        }
    }

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                i++
                append(
                    when (s[i]) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> s[i]
                    },
                )
            } else {
                append(c)
            }
            i++
        }
    }
}
