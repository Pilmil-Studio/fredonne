package fr.fredonne.core.music

/**
 * Codec ABC 2.1 autonome en Kotlin pur pour l'interopérabilité ouverte (MuseScore, EasyABC, abcjs).
 *
 * Produit une notation texte compacte compatible avec les QR codes, tout en conservant l'éventuel
 * instrument transpositeur Fredonne via la directive standard `%%fredonne-instrument:<id>`.
 */
object AbcCodec {

    private val UNIT_DURATION = Fraction.of(1, 8) // L:1/8
    private const val INSTRUMENT_DIRECTIVE = "%%fredonne-instrument:"
    private const val MAX_MEASURES = 500
    private const val MAX_TOTAL_EVENTS = 4096

    fun encode(
        melody: Melody,
        instrument: Instrument? = null,
        tempoBpm: Int = 96,
        title: String = "Partition Fredonne",
    ): String {
        val sb = StringBuilder()
        val safeTitle = title.replace('\r', ' ').replace('\n', ' ').trim().ifEmpty { "Partition Fredonne" }
        sb.appendLine("X:1")
        sb.appendLine("T:$safeTitle")
        sb.appendLine("M:${melody.time.beats}/${melody.time.unit}")
        sb.appendLine("L:1/8")
        sb.appendLine("Q:1/4=${tempoBpm.coerceIn(40, 200)}")
        if (instrument != null) {
            sb.appendLine("$INSTRUMENT_DIRECTIVE${instrument.id}")
        }
        sb.appendLine("K:${formatAbcKey(melody.key)}")

        val measuresText = melody.measures.map { measure ->
            encodeMeasure(measure, melody.key)
        }
        sb.append(measuresText.joinToString(" | "))
        sb.append(" |]")
        return sb.toString()
    }

    fun looksLikeAbc(text: String): Boolean {
        val lines = text.trim().lines()
        val hasX = lines.any { it.trimStart().startsWith("X:") }
        val hasK = lines.any { it.trimStart().startsWith("K:") }
        return hasX && hasK
    }

    fun decode(abcText: String): ShareCodec.SharedScore {
        val lines = abcText.replace("\r\n", "\n").lines()
        var key = Key(Step.C, 0, Mode.MAJOR)
        var time = TimeSignature(4, 4)
        var unitLength = UNIT_DURATION
        var tempoBpm = 96
        var instrument: Instrument? = null
        val bodyBuilder = StringBuilder()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith(INSTRUMENT_DIRECTIVE)) {
                val id = line.removePrefix(INSTRUMENT_DIRECTIVE).trim()
                if (id.isNotEmpty()) {
                    instrument = Instruments.byId(id)
                }
                continue
            }
            if (line.startsWith("%")) continue

            val headerMatch = HEADER_LINE_REGEX.matchEntire(line)
            if (headerMatch != null) {
                val field = headerMatch.groupValues[1]
                val value = headerMatch.groupValues[2].trim()
                when (field) {
                    "M" -> time = parseTimeSignature(value)
                    "L" -> unitLength = parseUnitLength(value)
                    "Q" -> tempoBpm = parseTempo(value)
                    "K" -> key = parseKey(value)
                }
                continue
            }

            if (bodyBuilder.isNotEmpty()) bodyBuilder.append(' ')
            bodyBuilder.append(line)
        }

        val measures = parseBody(bodyBuilder.toString(), key, unitLength)
        return ShareCodec.SharedScore(
            melody = Melody(key, time, measures.ifEmpty { listOf(Measure(emptyList())) }),
            instrument = instrument,
            tempoBpm = tempoBpm,
        )
    }

    private fun encodeMeasure(measure: Measure, key: Key): String {
        if (measure.events.isEmpty()) return ""
        val activeAccidentals = mutableMapOf<Pair<Step, Int>, Int>()
        val sb = StringBuilder()
        val events = measure.events
        var i = 0
        while (i < events.size) {
            val current = events[i]
            val tup = current.tuplet
            if (tup != null) {
                // Regroupe les événements consécutifs appartenant au même tuplet (ex. triolet (3CDE)
                var groupCount = 1
                while (
                    i + groupCount < events.size &&
                    groupCount < tup.actual &&
                    events[i + groupCount].tuplet == tup
                ) {
                    groupCount++
                }
                if (tup.actual == 3 && tup.normal == 2 && groupCount == 3) {
                    sb.append("(3")
                } else {
                    sb.append("(${tup.actual}:${tup.normal}:$groupCount")
                }
                for (k in 0 until groupCount) {
                    sb.append(encodeSingleEvent(events[i + k], key, activeAccidentals))
                }
                i += groupCount
            } else {
                sb.append(encodeSingleEvent(current, key, activeAccidentals))
                i++
            }
        }
        return sb.toString()
    }

    private fun encodeSingleEvent(
        event: Event,
        key: Key,
        activeAccidentals: MutableMap<Pair<Step, Int>, Int>,
    ): String {
        val durSuffix = encodeDurationSuffix(event.written)
        return when (event) {
            is Rest -> "z$durSuffix"
            is Note -> {
                val p = event.pitch
                val pitchKey = p.step to p.octave
                val expectedAlter = activeAccidentals[pitchKey] ?: key.diatonicAlter(p.step)
                val accPrefix = if (p.alter != expectedAlter) {
                    activeAccidentals[pitchKey] = p.alter
                    when (p.alter) {
                        2 -> "^^"
                        1 -> "^"
                        0 -> "="
                        -1 -> "_"
                        -2 -> "__"
                        else -> ""
                    }
                } else {
                    ""
                }
                val notePitch = encodeAbcPitch(p.step, p.octave)
                "$accPrefix$notePitch$durSuffix"
            }
        }
    }

    private fun encodeAbcPitch(step: Step, octave: Int): String = when {
        octave <= 4 -> step.name + ",".repeat(4 - octave)
        octave == 5 -> step.name.lowercase()
        else -> step.name.lowercase() + "'".repeat(octave - 5)
    }

    private fun encodeDurationSuffix(written: Fraction): String {
        val ratio = written / UNIT_DURATION
        return when {
            ratio == Fraction.ONE -> ""
            ratio.denominator == 1L -> ratio.numerator.toString()
            ratio.numerator == 1L -> "/${ratio.denominator}"
            else -> "${ratio.numerator}/${ratio.denominator}"
        }
    }

    private fun formatAbcKey(key: Key): String {
        val acc = when {
            key.tonicAlter > 0 -> "#".repeat(key.tonicAlter)
            key.tonicAlter < 0 -> "b".repeat(-key.tonicAlter)
            else -> ""
        }
        val mode = if (key.mode == Mode.MINOR) "m" else ""
        return "${key.tonicStep.name}$acc$mode"
    }

    private fun parseTimeSignature(raw: String): TimeSignature {
        val clean = raw.uppercase()
        if (clean == "C") return TimeSignature(4, 4)
        if (clean == "C|") return TimeSignature(2, 2)
        val parts = clean.split("/")
        val beats = parts.getOrNull(0)?.trim()?.toIntOrNull()?.coerceIn(1, 32) ?: 4
        val unit = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 4
        val validUnit = if (unit in setOf(1, 2, 4, 8, 16, 32, 64)) unit else 4
        return TimeSignature(beats, validUnit)
    }

    private fun parseUnitLength(raw: String): Fraction {
        val parts = raw.split("/")
        val n = parts.getOrNull(0)?.trim()?.toLongOrNull()?.coerceIn(1L, 64L) ?: 1L
        val d = parts.getOrNull(1)?.trim()?.toLongOrNull()?.coerceIn(1L, 64L) ?: 8L
        return Fraction.of(n, d)
    }

    private fun parseTempo(raw: String): Int {
        val eqIdx = raw.indexOf('=')
        val numPart = if (eqIdx >= 0) raw.substring(eqIdx + 1) else raw
        val bpm = numPart.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 96
        return bpm.coerceIn(40, 200)
    }

    private fun parseKey(raw: String): Key {
        val token = raw.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        if (token.isEmpty()) return Key(Step.C, 0, Mode.MAJOR)
        val stepChar = token[0].uppercaseChar()
        val step = Step.entries.firstOrNull { it.name == stepChar.toString() } ?: return Key(Step.C, 0, Mode.MAJOR)
        var pos = 1
        var alter = 0
        while (pos < token.length && (token[pos] == '#' || token[pos] == 'b')) {
            alter += if (token[pos] == '#') 1 else -1
            pos++
        }
        val rest = token.substring(pos).lowercase()
        val mode = if (rest.startsWith("m") && !rest.startsWith("maj")) Mode.MINOR else Mode.MAJOR
        return Key(step, alter, mode)
    }

    private fun parseBody(body: String, key: Key, unitLength: Fraction): List<Measure> {
        val measures = mutableListOf<Measure>()
        var currentEvents = mutableListOf<Event>()
        var activeAccidentals = mutableMapOf<Pair<Step, Int>, Int>()

        var pos = 0
        var totalEvents = 0
        var pendingTuplet: Tuplet? = null
        var tupletRemaining = 0

        fun flushMeasure(forceEmpty: Boolean = false) {
            if ((currentEvents.isNotEmpty() || forceEmpty) && measures.size < MAX_MEASURES) {
                measures += Measure(currentEvents)
                currentEvents = mutableListOf()
            } else {
                currentEvents.clear()
            }
            activeAccidentals = mutableMapOf()
            pendingTuplet = null
            tupletRemaining = 0
        }

        while (pos < body.length && measures.size < MAX_MEASURES && totalEvents < MAX_TOTAL_EVENTS) {
            val ch = body[pos]
            when {
                ch.isWhitespace() || ch == '-' || ch == ')' -> pos++
                ch == '|' -> {
                    // Consomme ||, |], |:, :|
                    while (pos < body.length && (body[pos] == '|' || body[pos] == ']' || body[pos] == ':')) {
                        pos++
                    }
                    flushMeasure(forceEmpty = currentEvents.isNotEmpty())
                }
                ch == ':' && pos + 1 < body.length && body[pos + 1] == '|' -> {
                    pos += 2
                    flushMeasure(forceEmpty = currentEvents.isNotEmpty())
                }
                ch == '[' && pos + 1 < body.length && body[pos + 1] == '|' -> {
                    pos += 2
                    flushMeasure(forceEmpty = currentEvents.isNotEmpty())
                }
                ch == '(' && pos + 1 < body.length && body[pos + 1].isDigit() -> {
                    pos++ // saute '('
                    val pStart = pos
                    while (pos < body.length && body[pos].isDigit()) pos++
                    val p = body.substring(pStart, pos).toIntOrNull()?.coerceAtLeast(2) ?: 3
                    var q = if (p % 2 == 1) 2 else 3
                    var r = p
                    if (pos < body.length && body[pos] == ':') {
                        pos++
                        val qStart = pos
                        while (pos < body.length && body[pos].isDigit()) pos++
                        body.substring(qStart, pos).toIntOrNull()?.let { if (it > 0) q = it }
                        if (pos < body.length && body[pos] == ':') {
                            pos++
                            val rStart = pos
                            while (pos < body.length && body[pos].isDigit()) pos++
                            body.substring(rStart, pos).toIntOrNull()?.let { if (it > 0) r = it }
                        }
                    }
                    pendingTuplet = Tuplet(p, q)
                    tupletRemaining = r
                }
                ch == '^' || ch == '_' || ch == '=' || ch in 'A'..'G' || ch in 'a'..'g' || ch == 'z' || ch == 'Z' || ch == 'x' -> {
                    var explicitAlter: Int? = null
                    if (ch == '^' || ch == '_' || ch == '=') {
                        explicitAlter = when (ch) {
                            '=' -> {
                                pos++
                                0
                            }
                            '^' -> {
                                var count = 0
                                while (pos < body.length && body[pos] == '^') {
                                    count++
                                    pos++
                                }
                                count.coerceAtMost(2)
                            }
                            '_' -> {
                                var count = 0
                                while (pos < body.length && body[pos] == '_') {
                                    count++
                                    pos++
                                }
                                -count.coerceAtMost(2)
                            }
                            else -> null
                        }
                    }

                    if (pos >= body.length) break
                    val letter = body[pos]
                    if (letter == 'z' || letter == 'Z' || letter == 'x') {
                        pos++
                        val (dur, nextPos) = parseDuration(body, pos, unitLength)
                        pos = nextPos
                        val tup = if (tupletRemaining > 0) {
                            tupletRemaining--
                            pendingTuplet
                        } else null
                        currentEvents += Rest(dur, tup)
                        totalEvents++
                    } else if (letter in 'A'..'G' || letter in 'a'..'g') {
                        pos++
                        val step = Step.valueOf(letter.uppercaseChar().toString())
                        var octave = if (letter in 'a'..'g') 5 else 4
                        while (pos < body.length && (body[pos] == '\'' || body[pos] == ',')) {
                            octave += if (body[pos] == '\'') 1 else -1
                            pos++
                        }
                        octave = octave.coerceIn(0, 9)
                        val pitchKey = step to octave
                        val alter = explicitAlter ?: activeAccidentals[pitchKey] ?: key.diatonicAlter(step)
                        if (explicitAlter != null) {
                            activeAccidentals[pitchKey] = explicitAlter
                        }
                        val (dur, nextPos) = parseDuration(body, pos, unitLength)
                        pos = nextPos
                        val tup = if (tupletRemaining > 0) {
                            tupletRemaining--
                            pendingTuplet
                        } else null
                        currentEvents += Note(Pitch(step, octave, alter), dur, tup)
                        totalEvents++
                    } else {
                        pos++
                    }
                }
                else -> pos++
            }
        }

        if (currentEvents.isNotEmpty()) {
            measures += Measure(currentEvents)
        }
        return measures
    }

    private fun parseDuration(body: String, startPos: Int, unitLength: Fraction): Pair<Fraction, Int> {
        var pos = startPos
        val numStart = pos
        while (pos < body.length && body[pos].isDigit()) pos++
        val num = if (pos > numStart) {
            body.substring(numStart, pos).toLongOrNull()?.coerceIn(1L, 64L) ?: 1L
        } else {
            1L
        }

        var den = 1L
        if (pos < body.length && body[pos] == '/') {
            var slashCount = 0
            while (pos < body.length && body[pos] == '/') {
                slashCount++
                pos++
            }
            val denStart = pos
            while (pos < body.length && body[pos].isDigit()) pos++
            den = if (pos > denStart) {
                body.substring(denStart, pos).toLongOrNull()?.coerceIn(1L, 64L) ?: 2L
            } else {
                1L shl slashCount.coerceAtMost(4)
            }
        }
        val written = unitLength * Fraction.of(num, den)
        return written to pos
    }

    private val HEADER_LINE_REGEX = Regex("^([A-Za-z]):(.*)$")
}
