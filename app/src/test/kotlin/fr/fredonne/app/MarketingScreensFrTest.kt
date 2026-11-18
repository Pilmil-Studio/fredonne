package fr.fredonne.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TimeSignature
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Captures pour les visuels marketing (docs/marketing/captures/fr) : mêmes écrans que [ScreenshotsTest],
 * mais à la résolution réelle de l'appareil (1080 x 2340) au lieu d'une réduction à 1000 px de haut.
 * Données fabriquées : « Frère Jacques », mélodie traditionnelle du domaine public.
 * L'appli n'existe qu'en français.
 */
class MarketingScreensFrTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "fr"),
        theme = "android:Theme.Material.Light.NoActionBar",
        maxPercentDifference = 0.1,
        useDeviceResolution = true,
    )

    init {
        Locale.setDefault(Locale.FRENCH)
    }

    private fun n(step: Step, duration: fr.fredonne.core.music.Fraction, lyric: String? = null) =
        Note(Pitch(step, 4), duration, lyric = lyric)

    private fun demoMelody(): Melody {
        val q = Durations.QUARTER
        val h = Durations.HALF
        return Melody(
            key = Key(Step.C, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(listOf(n(Step.C, q, "Frè"), n(Step.D, q, "re"), n(Step.E, q, "Ja"), n(Step.C, q, "cques"))),
                Measure(listOf(n(Step.C, q, "Frè"), n(Step.D, q, "re"), n(Step.E, q, "Ja"), n(Step.C, q, "cques"))),
                Measure(listOf(n(Step.E, q, "Dor"), n(Step.F, q, "mez"), n(Step.G, h, "vous"))),
                Measure(listOf(n(Step.E, q), n(Step.F, q), n(Step.G, h))),
            ),
        )
    }

    private fun capture(state: EditorState) {
        paparazzi.snapshot {
            FredonneApp(initialOnboarded = true, initialEditorState = state)
        }
    }

    @Test
    fun bienvenue() {
        paparazzi.snapshot { FredonneApp(initialOnboarded = false) }
    }

    @Test
    fun editeur() {
        capture(EditorState(melody = demoMelody(), cursorMeasure = 1, cursorEventIndex = 2))
    }

    @Test
    fun saisie() {
        val melody = demoMelody()
        val e = Durations.EIGHTH
        val withEighths = melody.copy(
            measures = melody.measures.take(2) + Measure(
                listOf(n(Step.E, e), n(Step.F, e), n(Step.G, Durations.QUARTER), n(Step.G, Durations.HALF)),
            ),
        )
        capture(
            EditorState(
                melody = withEighths,
                selectedDuration = Durations.EIGHTH,
                cursorMeasure = 2,
                cursorEventIndex = 2,
            ),
        )
    }

    @Test
    fun transposition() {
        capture(
            EditorState(
                melody = demoMelody(),
                instrument = Instruments.CLARINET_BB,
                cursorMeasure = 0,
                cursorEventIndex = 1,
            ),
        )
    }

    @Test
    fun repetition() {
        capture(
            EditorState(
                melody = demoMelody(),
                cursorMeasure = 1,
                cursorEventIndex = 4,
                loopEnabled = true,
                loopStartMeasure = 1,
                loopEndMeasure = 2,
                playbackSpeed = 0.75,
            ),
        )
    }
}
