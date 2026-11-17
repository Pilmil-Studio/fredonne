package fr.fredonne.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.fredonne.core.music.AudioMelodyTranscriber
import fr.fredonne.core.music.ChromaticTuner
import fr.fredonne.core.music.Clef
import fr.fredonne.core.music.Direction
import fr.fredonne.core.music.DoubleAlterationOnHarp
import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Fraction
import fr.fredonne.core.music.HarpPedals
import fr.fredonne.core.music.HighRoughnessInterval
import fr.fredonne.core.music.Instrument
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Issue
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.LeverChangeRequired
import fr.fredonne.core.music.MeasureTooLong
import fr.fredonne.core.music.MeasureTooShort
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.MetronomeEngine
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.MusicTheory
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.OutOfRange
import fr.fredonne.core.music.ParallelFifths
import fr.fredonne.core.music.ParallelOctaves
import fr.fredonne.core.music.PedalChangeRequired
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.PracticeLoop
import fr.fredonne.core.music.QrChunker
import fr.fredonne.core.music.QrCodeEncoder
import fr.fredonne.core.music.Rest
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TapTempoTracker
import fr.fredonne.core.music.TunerReading
import fr.fredonne.core.music.TuningStatus
import fr.fredonne.core.music.VoiceCrossing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.TimeSource

private val PaperBg = Color(0xFFFAF8F4)
private val CardBg = Color(0xFFFFFFFF)
private val InkColor = Color(0xFF1E1D1A)
private val MutedInk = Color(0xFF6B675E)
private val AccentSage = Color(0xFF285D4D)
private val WarnRed = Color(0xFFB33A3A)
private val WarnAmber = Color(0xFFB87018)
private val CursorTeal = Color(0xFF1F6E8C)

private val FredonneColorScheme = lightColorScheme(
    primary = AccentSage,
    onPrimary = Color.White,
    background = PaperBg,
    surface = CardBg,
    onBackground = InkColor,
    onSurface = InkColor,
    surfaceVariant = Color(0xFFEEECE6),
    onSurfaceVariant = MutedInk,
)

@Composable
fun FredonneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FredonneColorScheme, content = content)
}

private const val AUTOSAVE_DELAY_MS = 700L

private const val MIC_TOOL_TUNER = "tuner"
private const val MIC_TOOL_HUM = "hum"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FredonneApp(
    initialOnboarded: Boolean? = null,
    initialEditorState: EditorState? = null,
) {
    val prefs = rememberFredonnePrefs()
    val sessionStoreHolder = rememberSessionStore()
    var onboarded by remember { mutableStateOf(initialOnboarded ?: prefs.onboarded) }
    var finishOnboardingAfterMic by remember { mutableStateOf(false) }
    var pendingOnboardingInstrument by remember { mutableStateOf<Instrument?>(null) }
    var pendingOnboardingSample by remember { mutableStateOf(false) }
    val sessionRepository = remember { SessionRepository(sessionStoreHolder) }
    var state by remember {
        mutableStateOf(
            initialEditorState
                ?: sessionRepository.load()
                ?: EditorState(instrument = prefs.preferredInstrumentId?.let(Instruments::byId))
        )
    }

    // Enregistrement automatique : regroupé sur un court délai, forcé à la mise en arrière-plan.
    // Désactivé quand l'état initial est imposé (captures d'écran, tests).
    val autosave = remember { AutosaveScheduler(sessionRepository) }
    val persistable = state.copy(undoStack = emptyList(), redoStack = emptyList())
    LaunchedEffect(persistable, onboarded) {
        if (initialEditorState != null || !onboarded) return@LaunchedEffect
        val token = autosave.schedule(persistable)
        delay(AUTOSAVE_DELAY_MS)
        autosave.commit(token)
    }
    OnAppBackground { autosave.flush() }

    fun completeOnboarding(instrument: Instrument?, withSample: Boolean) {
        prefs.preferredInstrumentId = instrument?.id
        prefs.onboarded = true
        state = EditorState(
            melody = if (withSample) EditorState.sampleMelody() else EditorState.defaultMelody(),
            instrument = instrument,
            cursorMeasure = if (withSample) 1 else 0,
            cursorEventIndex = if (withSample) 3 else 0,
        )
        onboarded = true
    }

    var showTunerDialog by rememberSaveable { mutableStateOf(false) }
    var showHumDialog by rememberSaveable { mutableStateOf(false) }
    // Outil micro demandé avant l'autorisation : ouvert si elle est accordée, message sinon.
    var pendingMicTool by remember { mutableStateOf<String?>(null) }
    var showMicDeniedDialog by rememberSaveable { mutableStateOf(false) }
    val micPermission = rememberMicPermissionState { granted ->
        val tool = pendingMicTool
        if (tool != null) {
            pendingMicTool = null
            if (!granted) {
                showMicDeniedDialog = true
            } else if (tool == MIC_TOOL_TUNER) {
                showTunerDialog = true
            } else {
                showHumDialog = true
            }
        }
        if (finishOnboardingAfterMic) {
            finishOnboardingAfterMic = false
            completeOnboarding(pendingOnboardingInstrument, pendingOnboardingSample)
        }
    }

    if (!onboarded) {
        OnboardingScreen(
            initialInstrument = state.instrument,
            micGranted = micPermission.granted,
            onRequestMicAndFinish = { instrument, withSample ->
                pendingOnboardingInstrument = instrument
                pendingOnboardingSample = withSample
                finishOnboardingAfterMic = true
                micPermission.request()
            },
            onFinish = { instrument, withSample ->
                completeOnboarding(instrument, withSample)
            },
        )
        return
    }

    val player = remember { TonePlayer() }
    val exporter = rememberScoreExporter()
    var isPlaying by remember { mutableStateOf(false) }
    var playingPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showInstrumentDialog by rememberSaveable { mutableStateOf(false) }
    var showFormatsDialog by rememberSaveable { mutableStateOf(false) }
    var showRehearsalDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmClearDialog by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var readingMode by rememberSaveable { mutableStateOf(false) }
    var baseOctave by remember { mutableIntStateOf(4) }
    val latestState by rememberUpdatedState(state)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val playbackScope = rememberCoroutineScope()

    fun launchPlayback() {
        val current = latestState
        val loopOffset = current.activeLoop?.startMeasure ?: 0
        player.playMelody(
            melody = current.playbackMelody,
            bpm = current.effectivePlaybackBpm,
            onEventPlaying = { m, e ->
                playbackScope.launch(Dispatchers.Main.immediate) {
                    if (!latestIsPlaying) return@launch
                    val globalMeasure = m + loopOffset
                    playingPosition = globalMeasure to e
                    state = latestState.setCursor(globalMeasure, e + 1)
                }
            },
            onFinished = {
                playbackScope.launch(Dispatchers.Main.immediate) {
                    if (latestIsPlaying && latestState.loopEnabled) {
                        launchPlayback()
                    } else {
                        isPlaying = false
                        playingPosition = null
                    }
                }
            },
            timbre = ToneSynthesizer.timbreForInstrument(current.instrument),
            referenceA4Hz = current.tunerReferenceA4Hz,
        )
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Scaffold(
        containerColor = PaperBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaperBg,
                    titleContentColor = InkColor,
                ),
                title = {
                    Column {
                        Text(
                            text = "Fredonne",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                        )
                        Text(
                            text = "${formatKeyLabel(state.displayedMelody.key)} · ${state.melody.time.beats}/${state.melody.time.unit}" +
                                (state.instrument?.let { " · ${instrumentLabel(it)}" } ?: " · Sons réels (Ut)"),
                            fontSize = 12.sp,
                            color = MutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                player.stop()
                                isPlaying = false
                                playingPosition = null
                            } else {
                                isPlaying = true
                                launchPlayback()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Arrêter" else "Écouter",
                            tint = AccentSage,
                        )
                    }
                    IconButton(
                        onClick = { state = state.undo() },
                        enabled = state.canUndo && !isPlaying,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
                    }
                    IconButton(
                        onClick = { state = state.redo() },
                        enabled = state.canRedo && !isPlaying,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir")
                    }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (readingMode) "Mode édition (clavier)" else "Mode pupitre (lecture plein écran)") },
                            onClick = {
                                showOverflowMenu = false
                                readingMode = !readingMode
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Réglages & Micro") },
                            onClick = {
                                showOverflowMenu = false
                                showSettingsDialog = true
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Charger l'exemple (Frère Jacques)") },
                            onClick = {
                                showOverflowMenu = false
                                state = state.loadSampleMelody()
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Effacer la portée") },
                            onClick = {
                                showOverflowMenu = false
                                showConfirmClearDialog = true
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("À propos") },
                            onClick = {
                                showOverflowMenu = false
                                showAboutDialog = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentSage.copy(alpha = 0.08f),
                            PaperBg,
                            Color(0xFFF3EFE6),
                        ),
                    )
                )
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!readingMode) {
            // Barre de réglages : Tonalité / Transposition, Instrument, Exemple / Vider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { showKeyDialog = true },
                    label = {
                        Text("Tonalité : ${formatKeyLabel(state.melody.key)} (${state.melody.time.beats}/${state.melody.time.unit})")
                    },
                )
                AssistChip(
                    onClick = { showInstrumentDialog = true },
                    label = {
                        Text(state.instrument?.let { instrumentLabel(it) } ?: "Instrument : Concert (Ut)")
                    },
                )
                AssistChip(
                    onClick = { showFormatsDialog = true },
                    label = { Text("MusicXML · MIDI · PDF") },
                )
                AssistChip(
                    onClick = { showRehearsalDialog = true },
                    label = {
                        val suffix = buildString {
                            if (state.loopEnabled) append(" · M${state.loopStartMeasure + 1}-${state.loopEndMeasure + 1}")
                            if (state.playbackSpeed != 1.0) append(" · ${state.playbackSpeed}×")
                        }
                        Text("Répéter$suffix")
                    },
                )
                AssistChip(
                    onClick = {
                        if (micPermission.granted) {
                            showTunerDialog = true
                        } else {
                            pendingMicTool = MIC_TOOL_TUNER
                            micPermission.request()
                        }
                    },
                    label = { Text("Accordeur") },
                )
                AssistChip(
                    onClick = {
                        if (micPermission.granted) {
                            showHumDialog = true
                        } else {
                            pendingMicTool = MIC_TOOL_HUM
                            micPermission.request()
                        }
                    },
                    label = { Text("Fredonner") },
                )
                if (state.noteBeforeCursor != null) {
                    AssistChip(
                        onClick = { showLyricDialog = true },
                        label = {
                            val note = state.noteBeforeCursor
                            val detail = buildString {
                                if (!note?.lyric.isNullOrBlank()) append(" : ${note?.lyric}")
                                if (note?.breathMark == true) append(" '")
                            }
                            Text("Parole · Resp.$detail")
                        },
                    )
                }
            }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mode pupitre · ${state.tempoBpm} BPM",
                        fontSize = 12.sp,
                        color = MutedInk,
                    )
                    TextButton(onClick = { readingMode = false }) {
                        Text("Éditer")
                    }
                }
            }

            // Portée interactive
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = CardBg,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE0DDD5)),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    StaffCanvas(
                        state = state,
                        playingPosition = playingPosition,
                        onSelectCursor = { m, e -> state = state.setCursor(m, e) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    HorizontalDivider(color = Color(0xFFECE9E2))
                    VerifierBar(issues = state.issues, tempoBpm = state.tempoBpm)
                }
            }

            if (!readingMode) {
            // Barre de tempo compacte + déplacement du curseur
            Row(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Tempo ${state.tempoBpm}", fontSize = 12.sp, color = MutedInk, modifier = Modifier.width(72.dp))
                Slider(
                    value = state.tempoBpm.toFloat(),
                    onValueChange = { state = state.setTempo(it.roundToInt()) },
                    valueRange = 50f..160f,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { state = state.moveCursorLeft() },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Curseur gauche")
                }
                IconButton(
                    onClick = { state = state.moveCursorRight() },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Curseur droit")
                }
                val backspaceInteraction = remember { MutableInteractionSource() }
                val isBackspacePressed by backspaceInteraction.collectIsPressedAsState()
                val latestDelete by rememberUpdatedState { state = state.deleteAtCursor() }
                // Appui simple : une note (action TalkBack nommée) ; appui maintenu : effacement en continu.
                LaunchedEffect(isBackspacePressed) {
                    if (isBackspacePressed) {
                        delay(360L)
                        while (isBackspacePressed) {
                            latestDelete()
                            delay(90L)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            interactionSource = backspaceInteraction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Supprimer la note avant le curseur",
                            onLongClickLabel = "Effacer en continu",
                            // Au toucher, la boucle ci-dessus gère l'appui long ; l'action d'accessibilité supprime une note.
                            onLongClick = { if (!isBackspacePressed) latestDelete() },
                            onClick = { latestDelete() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Supprimer la note. Maintenir l'appui pour effacer en continu.",
                    )
                }
            }

            // Barre de durées + silence + point + mesure
            Box(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            ) {
                DurationToolbar(
                    selectedDuration = state.selectedDuration,
                    dotted = state.dotted,
                    onSelectDuration = { state = state.setDuration(it) },
                    onToggleDotted = { state = state.toggleDotted() },
                    onInsertRest = { state = state.insertRest() },
                    onAddMeasure = { state = state.addMeasure() },
                )
            }

            // Mini clavier piano pour saisie directe avec retour sonore immédiat
            Box(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            ) {
                PianoKeyboard(
                    baseOctave = baseOctave,
                    onShiftOctave = { delta -> baseOctave = (baseOctave + delta).coerceIn(1, 5) },
                    onKeyPressed = { pitch ->
                        player.playSingleNote(
                            midi = pitch.midi,
                            timbre = ToneSynthesizer.timbreForInstrument(state.instrument),
                            referenceA4Hz = state.tunerReferenceA4Hz,
                        )
                        state = state.insertNote(pitch)
                    },
                )
            }
            }
        }
    }

    if (showKeyDialog) {
        KeyAndTransposeDialog(
            state = state,
            onDismiss = { showKeyDialog = false },
            onUpdateState = { state = it },
        )
    }

    if (showInstrumentDialog) {
        InstrumentDialog(
            current = state.instrument,
            onSelect = { inst ->
                state = state.setInstrument(inst)
                if (inst != null) {
                    baseOctave = when (inst.id) {
                        "bass_electric", "contrabass" -> 1
                        "voice_bass", "trombone" -> 2
                        "voice_tenor", "harp_celtic", "guitar", "horn_f", "sax_baritone" -> 3
                        else -> 4
                    }
                }
                showInstrumentDialog = false
            },
            onDismiss = { showInstrumentDialog = false },
        )
    }

    if (showFormatsDialog) {
        FormatsDialog(
            state = state,
            exporter = exporter,
            onUpdateState = {
                state = it
                showFormatsDialog = false
            },
            onDismiss = { showFormatsDialog = false },
        )
    }

    if (showRehearsalDialog) {
        RehearsalDialog(
            state = state,
            player = player,
            onUpdateState = { state = it },
            onDismiss = { showRehearsalDialog = false },
        )
    }

    if (showLyricDialog) {
        val targetNote = state.noteBeforeCursor
        if (targetNote != null) {
            LyricAndBreathDialog(
                note = targetNote,
                onSave = { lyric, breath ->
                    var updated = state.setNoteLyricBeforeCursor(lyric)
                    if (updated.noteBeforeCursor?.breathMark != breath) {
                        updated = updated.toggleBreathMarkBeforeCursor()
                    }
                    state = updated
                    showLyricDialog = false
                },
                onDismiss = { showLyricDialog = false },
            )
        } else {
            showLyricDialog = false
        }
    }

    if (showTunerDialog && micPermission.granted) {
        TunerDialog(
            referenceA4Hz = state.tunerReferenceA4Hz,
            onDismiss = { showTunerDialog = false },
        )
    }

    if (showMicDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showMicDeniedDialog = false },
            title = { Text("Micro refusé") },
            text = { Text("L'accordeur et « Fredonner » ont besoin du micro. Vous pouvez l'autoriser dans les réglages du système.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicDeniedDialog = false
                        micPermission.openSettings()
                    },
                ) { Text("Réglages système") }
            },
            dismissButton = { TextButton(onClick = { showMicDeniedDialog = false }) { Text("Fermer") } },
        )
    }

    if (showHumDialog && micPermission.granted) {
        HumDialog(
            state = state,
            onApplyMelody = { transcribed ->
                state = state.applyTranscribedMelody(transcribed)
                showHumDialog = false
            },
            onDismiss = { showHumDialog = false },
        )
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Effacer la portée ?") },
            text = { Text("Toutes les notes saisies seront supprimées (vous pourrez encore utiliser Annuler).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        player.stop()
                        isPlaying = false
                        playingPosition = null
                        state = state.clearAll()
                        showConfirmClearDialog = false
                    },
                ) { Text("Effacer") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) { Text("Annuler") }
            },
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            referenceA4Hz = state.tunerReferenceA4Hz,
            onChangeReferenceA4 = { hz -> state = state.setTunerReferenceA4Hz(hz) },
            micPermission = micPermission,
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun StaffCanvas(
    state: EditorState,
    playingPosition: Pair<Int, Int>?,
    onSelectCursor: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val melody = state.displayedMelody
    val issues = state.issues
    val textMeasurer = rememberTextMeasurer()
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(320f)
        val viewportHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(150f)

        val clefAreaPx = with(density) { 82.dp.toPx() }
        val eventStepPx = with(density) { 42.dp.toPx() }
        val measurePadPx = with(density) { 20.dp.toPx() }
        val minMeasureWidthPx = with(density) { 112.dp.toPx() }
        val rightMarginPx = with(density) { 16.dp.toPx() }
        val baseSystemHeightPx = with(density) { 142.dp.toPx() }

        // Découpage automatique en lignes de portée (systèmes) dès qu'une mesure dépasse la largeur de l'écran
        val systemLayouts = remember(
            melody,
            viewportWidthPx,
            clefAreaPx,
            eventStepPx,
            measurePadPx,
            minMeasureWidthPx,
            rightMarginPx,
        ) {
            val usableLineWidth = (viewportWidthPx - clefAreaPx - rightMarginPx).coerceAtLeast(minMeasureWidthPx)

            data class RawMeasureSpec(
                val index: Int,
                val eventSteps: List<Float>,
                val naturalWidth: Float,
            )

            val specs = melody.measures.mapIndexed { mIdx, measure ->
                val steps = measure.events.map { event ->
                    eventStepPx * MusicTheory.gourlaySpacingFactor(event.length)
                }
                val natW = maxOf(minMeasureWidthPx, steps.sum() + measurePadPx * 2f)
                RawMeasureSpec(mIdx, steps, natW)
            }

            val rows = mutableListOf<MutableList<RawMeasureSpec>>()
            var currentRow = mutableListOf<RawMeasureSpec>()
            var currentRowWidth = 0f

            for (spec in specs) {
                if (currentRow.isNotEmpty() && currentRowWidth + spec.naturalWidth > usableLineWidth) {
                    rows += currentRow
                    currentRow = mutableListOf(spec)
                    currentRowWidth = spec.naturalWidth
                } else {
                    currentRow += spec
                    currentRowWidth += spec.naturalWidth
                }
            }
            if (currentRow.isNotEmpty()) rows += currentRow

            rows.mapIndexed { sysIdx, rowSpecs ->
                val rawSum = rowSpecs.sumOf { it.naturalWidth.toDouble() }.toFloat().coerceAtLeast(1f)
                // Si la ligne comporte plusieurs mesures ou remplit plus de 75 % de la largeur, on l'étire jusqu'à la marge droite
                val stretch = if (rowSpecs.size > 1 || rawSum > usableLineWidth * 0.75f) {
                    (usableLineWidth / rawSum).coerceIn(0.85f, 1.35f)
                } else {
                    1.0f
                }

                var x = clefAreaPx
                val layouts = rowSpecs.map { spec ->
                    val startX = x
                    val mWidth = spec.naturalWidth * stretch
                    val pad = measurePadPx * stretch
                    var cursorOffset = startX + pad
                    val eventXs = spec.eventSteps.map { stepW ->
                        val scaledStep = stepW * stretch
                        val cx = cursorOffset + scaledStep * 0.45f
                        cursorOffset += scaledStep
                        cx
                    }
                    val endX = startX + mWidth
                    x = endX
                    MeasureLayout(spec.index, sysIdx, startX, endX, eventXs)
                }
                SystemLayout(sysIdx, layouts)
            }
        }

        val systemHeightPx = if (systemLayouts.size <= 1) {
            maxOf(baseSystemHeightPx, viewportHeightPx)
        } else {
            baseSystemHeightPx
        }
        val totalCanvasHeightDp = with(density) {
            (systemLayouts.size * systemHeightPx + 12f).toDp()
        }

        // Autoscroll vertical vers la ligne de portée active lors de la saisie ou de la lecture
        val activeMeasureIndex = playingPosition?.first ?: state.cursorMeasure
        val activeEventIndex = playingPosition?.second ?: state.cursorEventIndex
        LaunchedEffect(activeMeasureIndex, activeEventIndex, systemLayouts.size) {
            val activeSystemIdx = systemLayouts.indexOfFirst { sys ->
                sys.measures.any { it.index == activeMeasureIndex }
            }
            if (activeSystemIdx >= 0 && systemLayouts.size > 1) {
                val sysTop = (activeSystemIdx * systemHeightPx).roundToInt()
                val sysBottom = ((activeSystemIdx + 1) * systemHeightPx).roundToInt()
                val viewTop = verticalScrollState.value
                val viewBottom = viewTop + viewportHeightPx.roundToInt()
                if (sysBottom > viewBottom) {
                    verticalScrollState.animateScrollTo((sysBottom - viewportHeightPx.roundToInt()).coerceAtLeast(0))
                } else if (sysTop < viewTop) {
                    verticalScrollState.animateScrollTo(sysTop)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalCanvasHeightDp)
                    .pointerInput(systemLayouts, melody, systemHeightPx) {
                        detectTapGestures { offset ->
                            val sysIdx = (offset.y / systemHeightPx).toInt().coerceIn(0, (systemLayouts.size - 1).coerceAtLeast(0))
                            val system = systemLayouts.getOrNull(sysIdx)
                            val clickedMeasure = system?.measures?.firstOrNull { offset.x in it.startX..it.endX }
                                ?: system?.measures?.lastOrNull()
                            if (clickedMeasure != null) {
                                val events = clickedMeasure.eventXs
                                val targetEvent = if (events.isEmpty()) {
                                    0
                                } else {
                                    var idx = events.indexOfFirst { offset.x < it }
                                    if (idx == -1) idx = events.size
                                    idx
                                }
                                onSelectCursor(clickedMeasure.index, targetEvent)
                            }
                        }
                    },
            ) {
                val lineSpacing = (systemHeightPx * 0.095f).coerceIn(12f, 20f)
                val stepHalf = lineSpacing / 2f
                val clef = Clef.resolveFor(melody, state.instrument)
                val bottomLineDiatonic = clef.bottomLinePitch.diatonic

                systemLayouts.forEach { system ->
                    val sysTopOffset = system.systemIndex * systemHeightPx
                    val staffTop = sysTopOffset + systemHeightPx * 0.28f
                    val bottomLineY = staffTop + 4 * lineSpacing
                    val systemEndX = system.measures.lastOrNull()?.endX ?: (size.width - 16f)

                    fun yForPitch(pitch: Pitch): Float {
                        val delta = pitch.diatonic - bottomLineDiatonic
                        return bottomLineY - delta * stepHalf
                    }

                    // 5 lignes de la portée pour ce système
                    for (line in 0..4) {
                        val y = staffTop + line * lineSpacing
                        drawLine(
                            color = InkColor.copy(alpha = 0.78f),
                            start = Offset(16f, y),
                            end = Offset(systemEndX, y),
                            strokeWidth = 2f,
                        )
                    }

                    // Barre de début de système
                    drawLine(
                        color = AccentSage.copy(alpha = 0.85f),
                        start = Offset(16f, staffTop),
                        end = Offset(16f, bottomLineY),
                        strokeWidth = 2.6f,
                    )

                    // Clé (Sol, Sol 8vb, Fa, Ut 3 ou Ut 4) + armure + chiffrage
                    drawText(
                        textMeasurer = textMeasurer,
                        text = clef.displaySymbol,
                        topLeft = Offset(
                            20f,
                            staffTop + lineSpacing * when (clef) {
                                Clef.BASS -> 0.15f
                                Clef.ALTO, Clef.TENOR -> 0.35f
                                else -> 0.55f
                            },
                        ),
                        style = TextStyle(
                            color = AccentSage,
                            fontSize = if (clef == Clef.TREBLE) 25.sp else 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    val keyFifths = melody.key.fifths
                    val accLabel = when {
                        keyFifths > 0 -> "${keyFifths}#"
                        keyFifths < 0 -> "${-keyFifths}b"
                        else -> ""
                    }
                    if (accLabel.isNotEmpty()) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = accLabel,
                            topLeft = Offset(44f, staffTop - lineSpacing * 0.7f),
                            style = TextStyle(color = MutedInk, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                    if (system.systemIndex == 0) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${melody.time.beats}\n${melody.time.unit}",
                            topLeft = Offset(58f, staffTop + lineSpacing * 0.25f),
                            style = TextStyle(
                                color = InkColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp,
                            ),
                        )
                    }

                    // Mesures, barres, notes, silences et curseur
                    system.measures.forEach { layout ->
                        val measure = melody.measures[layout.index]
                        val measureIssues = issues.filter { it.measure == layout.index }
                        val hasLengthIssue = measureIssues.any { it is MeasureTooShort || it is MeasureTooLong }

                        if (hasLengthIssue && measure.events.isNotEmpty()) {
                            drawLine(
                                color = WarnAmber.copy(alpha = 0.75f),
                                start = Offset(layout.startX + 4f, bottomLineY + lineSpacing * 2.1f),
                                end = Offset(layout.endX - 4f, bottomLineY + lineSpacing * 2.1f),
                                strokeWidth = 3.5f,
                            )
                        }

                        // Barre de mesure à droite
                        drawLine(
                            color = InkColor.copy(alpha = 0.7f),
                            start = Offset(layout.endX, staffTop),
                            end = Offset(layout.endX, bottomLineY),
                            strokeWidth = if (layout.index == melody.measures.lastIndex) 3.5f else 2f,
                        )

                        // Numéro de mesure
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${layout.index + 1}",
                            topLeft = Offset(layout.startX + 6f, staffTop - lineSpacing * 1.35f),
                            style = TextStyle(color = MutedInk, fontSize = 10.sp),
                        )

                        measure.events.forEachIndexed { eIdx, event ->
                            val cx = layout.eventXs[eIdx]
                            val isPlayingNow = playingPosition == (layout.index to eIdx)
                            val isOutOfRange = measureIssues.any {
                                (it is OutOfRange && it.eventIndex == eIdx) ||
                                    (it is DoubleAlterationOnHarp && it.eventIndex == eIdx)
                            }
                            val isLeverChange = measureIssues.any { it is LeverChangeRequired && it.eventIndex == eIdx }
                            val pedalIssue = measureIssues.filterIsInstance<PedalChangeRequired>().firstOrNull { it.eventIndex == eIdx }

                            when (event) {
                                is Note -> {
                                    val cy = yForPitch(event.pitch)
                                    val noteColor = when {
                                        isPlayingNow -> CursorTeal
                                        isOutOfRange -> WarnRed
                                        isLeverChange || pedalIssue != null -> WarnAmber
                                        else -> InkColor
                                    }
                                    val d = event.pitch.diatonic
                                    val lowLineD = bottomLineDiatonic
                                    val highLineD = bottomLineDiatonic + 8
                                    if (d <= lowLineD - 2) {
                                        var ld = lowLineD - 2
                                        while (ld >= d) {
                                            val ly = bottomLineY - (ld - bottomLineDiatonic) * stepHalf
                                            drawLine(InkColor, Offset(cx - 12f, ly), Offset(cx + 12f, ly), strokeWidth = 2f)
                                            ld -= 2
                                        }
                                    } else if (d >= highLineD + 2) {
                                        var ld = highLineD + 2
                                        while (ld <= d) {
                                            val ly = bottomLineY - (ld - bottomLineDiatonic) * stepHalf
                                            drawLine(InkColor, Offset(cx - 12f, ly), Offset(cx + 12f, ly), strokeWidth = 2f)
                                            ld += 2
                                        }
                                    }

                                    if (event.pitch.alter != 0) {
                                        val symbol = if (event.pitch.alter > 0) "#" else "b"
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = symbol,
                                            topLeft = Offset(cx - 20f, cy - 11f),
                                            style = TextStyle(color = noteColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        )
                                    }

                                    val isWhole = event.written >= Durations.WHOLE
                                    val isHalf = event.written >= Durations.HALF && event.written < Durations.WHOLE
                                    val rx = lineSpacing * 0.62f
                                    val ry = lineSpacing * 0.44f

                                    if (isWhole || isHalf) {
                                        drawOval(
                                            color = noteColor,
                                            topLeft = Offset(cx - rx, cy - ry),
                                            size = Size(rx * 2, ry * 2),
                                            style = Stroke(width = 2.8f),
                                        )
                                    } else {
                                        drawOval(
                                            color = noteColor,
                                            topLeft = Offset(cx - rx, cy - ry),
                                            size = Size(rx * 2, ry * 2),
                                        )
                                    }

                                    if (!isWhole) {
                                        val stemUp = d < bottomLineDiatonic + 4
                                        val stemX = if (stemUp) cx + rx - 1.5f else cx - rx + 1.5f
                                        val stemEndY = if (stemUp) cy - lineSpacing * 2.75f else cy + lineSpacing * 2.75f
                                        drawLine(noteColor, Offset(stemX, cy), Offset(stemX, stemEndY), strokeWidth = 2.3f)

                                        val flags = when {
                                            event.written <= Durations.SIXTEENTH -> 2
                                            event.written <= Durations.EIGHTH -> 1
                                            else -> 0
                                        }
                                        repeat(flags) { f ->
                                            val fy = if (stemUp) stemEndY + f * 6.5f else stemEndY - f * 6.5f
                                            val path = Path().apply {
                                                moveTo(stemX, fy)
                                                quadraticTo(stemX + 11f, fy + if (stemUp) 7f else -7f, stemX + 7f, fy + if (stemUp) 15f else -15f)
                                            }
                                            drawPath(path, color = noteColor, style = Stroke(width = 2.1f))
                                        }
                                    }

                                    if (event.written.numerator == 3L && event.written.denominator >= 4L) {
                                        drawCircle(noteColor, radius = 3.0f, center = Offset(cx + rx + 5.5f, cy - 2f))
                                    }

                                    if (pedalIssue != null) {
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = pedalIssue.pedalLabel,
                                            topLeft = Offset(cx - 12f, bottomLineY + lineSpacing * 0.55f),
                                            style = TextStyle(color = WarnAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        )
                                    }

                                    if (event.breathMark) {
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = "'",
                                            topLeft = Offset(cx + rx + 4f, staffTop - lineSpacing * 1.15f),
                                            style = TextStyle(color = AccentSage, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                        )
                                    }

                                    val lyricText = event.lyric?.trim().orEmpty()
                                    if (lyricText.isNotEmpty()) {
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = lyricText,
                                            topLeft = Offset(cx - (lyricText.length * 3.1f), bottomLineY + lineSpacing * 1.45f),
                                            style = TextStyle(color = InkColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        )
                                    }
                                }
                                is Rest -> {
                                    val restColor = if (isPlayingNow) CursorTeal else MutedInk
                                    drawRect(
                                        color = restColor,
                                        topLeft = Offset(cx - 6.5f, staffTop + lineSpacing * 1.5f),
                                        size = Size(13f, 5.5f),
                                    )
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = "z",
                                        topLeft = Offset(cx - 4.5f, staffTop + lineSpacing * 2.2f),
                                        style = TextStyle(color = restColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }

                        // Curseur : tête de lecture sur la note courante pendant l'écoute, ou curseur d'insertion à l'arrêt
                        if (playingPosition != null) {
                            if (playingPosition.first == layout.index && layout.eventXs.isNotEmpty()) {
                                val playIdx = playingPosition.second.coerceIn(0, layout.eventXs.lastIndex)
                                val playX = layout.eventXs[playIdx]
                                val bandHalfW = (eventStepPx * 0.36f).coerceIn(10f, 18f)
                                drawRect(
                                    color = CursorTeal.copy(alpha = 0.14f),
                                    topLeft = Offset(playX - bandHalfW, staffTop - lineSpacing * 0.85f),
                                    size = Size(bandHalfW * 2f, (bottomLineY - staffTop) + lineSpacing * 1.7f),
                                )
                                drawLine(
                                    color = CursorTeal,
                                    start = Offset(playX, staffTop - lineSpacing * 0.9f),
                                    end = Offset(playX, bottomLineY + lineSpacing * 0.9f),
                                    strokeWidth = 3f,
                                )
                                drawCircle(
                                    color = CursorTeal,
                                    radius = 4.2f,
                                    center = Offset(playX, staffTop - lineSpacing * 0.95f),
                                )
                            }
                        } else if (state.cursorMeasure == layout.index) {
                            val cursorX = when {
                                measure.events.isEmpty() -> layout.startX + measurePadPx
                                state.cursorEventIndex == 0 -> layout.eventXs.first() - eventStepPx * 0.42f
                                state.cursorEventIndex >= measure.events.size -> layout.eventXs.last() + eventStepPx * 0.42f
                                else -> (layout.eventXs[state.cursorEventIndex - 1] + layout.eventXs[state.cursorEventIndex]) / 2f
                            }
                            drawLine(
                                color = CursorTeal,
                                start = Offset(cursorX, staffTop - lineSpacing * 0.85f),
                                end = Offset(cursorX, bottomLineY + lineSpacing * 0.85f),
                                strokeWidth = 3f,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MeasureLayout(
    val index: Int,
    val systemIndex: Int,
    val startX: Float,
    val endX: Float,
    val eventXs: List<Float>,
)

private data class SystemLayout(
    val systemIndex: Int,
    val measures: List<MeasureLayout>,
)

@Composable
private fun VerifierBar(issues: List<Issue>, tempoBpm: Int) {
    val message = when (val first = issues.firstOrNull()) {
        null -> "Mesures et tessiture conformes · $tempoBpm BPM"
        is MeasureTooShort -> "Mesure ${first.measure + 1} : incomplète (manque ${first.missing.numerator}/${first.missing.denominator})"
        is MeasureTooLong -> "Mesure ${first.measure + 1} : dépassement de ${first.excess.numerator}/${first.excess.denominator}"
        is OutOfRange -> "Mesure ${first.measure + 1} : note ${if (first.tooLow) "trop grave" else "trop aiguë"} pour l'instrument"
        is DoubleAlterationOnHarp -> "Mesure ${first.measure + 1} : double altération impossible à la harpe"
        is LeverChangeRequired -> "Mesure ${first.measure + 1} : altération hors armure (changement de levier requis)"
        is PedalChangeRequired -> "Mesure ${first.measure + 1} : pédale ${first.pedalLabel} (toutes les octaves)"
        is ParallelFifths -> "Mesure ${first.measure + 1} : quintes parallèles détectées"
        is ParallelOctaves -> "Mesure ${first.measure + 1} : octaves parallèles détectées"
        is VoiceCrossing -> "Mesure ${first.measure + 1} : croisement de voix"
        is HighRoughnessInterval -> "Mesure ${first.measure + 1} : intervalle à forte rugosité acoustique"
    }
    val textColor = when (issues.firstOrNull()) {
        null -> AccentSage
        is OutOfRange, is ParallelFifths, is ParallelOctaves -> WarnRed
        else -> WarnAmber
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (issues.size > 1) {
            Text(
                text = "+${issues.size - 1}",
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DurationToolbar(
    selectedDuration: Fraction,
    dotted: Boolean,
    onSelectDuration: (Fraction) -> Unit,
    onToggleDotted: () -> Unit,
    onInsertRest: () -> Unit,
    onAddMeasure: () -> Unit,
) {
    val durations = listOf(
        Durations.WHOLE to "Ronde",
        Durations.HALF to "Blanche",
        Durations.QUARTER to "Noire",
        Durations.EIGHTH to "Croche",
        Durations.SIXTEENTH to "Double",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        durations.forEach { (dur, label) ->
            FilterChip(
                selected = selectedDuration == dur,
                onClick = { onSelectDuration(dur) },
                label = { Text(label, fontSize = 12.sp) },
            )
        }
        FilterChip(
            selected = dotted,
            onClick = onToggleDotted,
            label = { Text("Pointée", fontSize = 12.sp) },
        )
        OutlinedButton(
            onClick = onInsertRest,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text("Silence", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onAddMeasure,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Mesure", fontSize = 12.sp)
        }
    }
}

@Composable
private fun PianoKeyboard(
    baseOctave: Int,
    onShiftOctave: (Int) -> Unit,
    onKeyPressed: (Pitch) -> Unit,
) {
    val whiteSteps = listOf(Step.C, Step.D, Step.E, Step.F, Step.G, Step.A, Step.B)
    val whiteKeys = (0..1).flatMap { octOffset ->
        whiteSteps.map { step -> Pitch(step, baseOctave + octOffset) }
    }
    // Touches noires positionnées entre les touches blanches (index de la touche blanche à gauche)
    val blackKeyDefs = listOf(
        0 to Pitch(Step.C, baseOctave, 1),
        1 to Pitch(Step.D, baseOctave, 1),
        3 to Pitch(Step.F, baseOctave, 1),
        4 to Pitch(Step.G, baseOctave, 1),
        5 to Pitch(Step.A, baseOctave, 1),
        7 to Pitch(Step.C, baseOctave + 1, 1),
        8 to Pitch(Step.D, baseOctave + 1, 1),
        10 to Pitch(Step.F, baseOctave + 1, 1),
        11 to Pitch(Step.G, baseOctave + 1, 1),
        12 to Pitch(Step.A, baseOctave + 1, 1),
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Clavier (Do$baseOctave – Si${baseOctave + 1})",
                fontSize = 12.sp,
                color = MutedInk,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onShiftOctave(-1) },
                    enabled = baseOctave > 1,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("-1 oct", fontSize = 12.sp)
                }
                TextButton(
                    onClick = { onShiftOctave(+1) },
                    enabled = baseOctave < 5,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("+1 oct", fontSize = 12.sp)
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .shadow(3.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFD5D1C7), RoundedCornerShape(10.dp)),
        ) {
            val whiteWidth: Dp = maxWidth / whiteKeys.size
            Row(modifier = Modifier.fillMaxSize()) {
                whiteKeys.forEach { pitch ->
                    Box(
                        modifier = Modifier
                            .width(whiteWidth)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF6F4EE), Color.White, Color(0xFFEDEAE2)),
                                )
                            )
                            .border(0.5.dp, Color(0xFFD8D5CC))
                            .clickable { onKeyPressed(pitch) }
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(
                            text = frenchNoteName(pitch.step) + if (pitch.step == Step.C) "${pitch.octave}" else "",
                            fontSize = 10.sp,
                            color = MutedInk,
                        )
                    }
                }
            }

            val blackWidth = whiteWidth * 0.62f
            blackKeyDefs.forEach { (leftWhiteIdx, pitch) ->
                val leftOffset = whiteWidth * (leftWhiteIdx + 1) - blackWidth / 2
                Box(
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .width(blackWidth)
                        .fillMaxHeight(0.58f)
                        .shadow(2.dp, RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                        .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF34322D), Color(0xFF1A1916)),
                            )
                        )
                        .clickable { onKeyPressed(pitch) },
                )
            }
        }
    }
}

@Composable
private fun KeyAndTransposeDialog(
    state: EditorState,
    onDismiss: () -> Unit,
    onUpdateState: (EditorState) -> Unit,
) {
    var selectedDirection by remember { mutableStateOf(Direction.NEAREST) }
    val keys = listOf(
        Triple(Step.C, 0, "Do majeur"),
        Triple(Step.G, 0, "Sol majeur (1#)"),
        Triple(Step.D, 0, "Ré majeur (2#)"),
        Triple(Step.A, 0, "La majeur (3#)"),
        Triple(Step.F, 0, "Fa majeur (1b)"),
        Triple(Step.B, -1, "Sib majeur (2b)"),
        Triple(Step.E, -1, "Mib majeur (3b)"),
    )
    val signatures = listOf(4 to 4, 3 to 4, 2 to 4, 6 to 8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tonalité, chiffrage & transposition") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Ajuster la hauteur à l'oreille (voix / confort) :",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeSemitones(-2)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text("-1 ton", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeSemitones(-1)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text("-½ ton", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeSemitones(1)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text("+½ ton", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeSemitones(2)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text("+1 ton", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeOctave(up = false)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("-1 octave")
                    }
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeOctave(up = true)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("+1 octave")
                    }
                }
                HorizontalDivider()
                Text("Transposer la mélodie vers une tonalité :", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Direction.NEAREST to "Plus proche",
                        Direction.UP to "Monter ↑",
                        Direction.DOWN to "Descendre ↓",
                    ).forEach { (dir, label) ->
                        FilterChip(
                            selected = selectedDirection == dir,
                            onClick = { selectedDirection = dir },
                            label = { Text(label, fontSize = 11.sp) },
                        )
                    }
                }
                keys.forEach { (step, alter, label) ->
                    val isCurrent = state.melody.key.tonicStep == step && state.melody.key.tonicAlter == alter
                    OutlinedButton(
                        onClick = { onUpdateState(state.transposeToKey(step, alter, selectedDirection)) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (isCurrent) AccentSage else Color(0xFFD5D1C7)),
                    ) {
                        Text(label, color = if (isCurrent) AccentSage else InkColor)
                    }
                }
                OutlinedButton(
                    onClick = { onUpdateState(state.applyDetectedKey()) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AccentSage.copy(alpha = 0.6f)),
                ) {
                    Text(
                        "Tonalité estimée : ${formatKeyLabel(state.detectedKey)} (adapter l'armure)",
                        color = AccentSage,
                        fontSize = 12.sp,
                    )
                }
                HorizontalDivider()
                Text("Chiffrage de mesure :", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    signatures.forEach { (b, u) ->
                        FilterChip(
                            selected = state.melody.time.beats == b && state.melody.time.unit == u,
                            onClick = { onUpdateState(state.setTimeSignature(b, u)) },
                            label = { Text("$b/$u") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun InstrumentDialog(
    current: Instrument?,
    onSelect: (Instrument?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options: List<Instrument?> = listOf(null) + Instruments.ALL
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instrument & transposition écrite") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { inst ->
                    val selected = current?.id == inst?.id
                    OutlinedButton(
                        onClick = { onSelect(inst) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (selected) AccentSage else Color(0xFFD5D1C7)),
                    ) {
                        Text(
                            text = inst?.let { instrumentLabel(it) } ?: "Concert (Ut, sons réels)",
                            color = if (selected) AccentSage else InkColor,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun OnboardingScreen(
    initialInstrument: Instrument?,
    micGranted: Boolean,
    onRequestMicAndFinish: (Instrument?, withSample: Boolean) -> Unit,
    onFinish: (Instrument?, withSample: Boolean) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var chosenInstrument by remember { mutableStateOf(initialInstrument) }
    var chosenWithSample by remember { mutableStateOf(false) }
    val quickInstruments: List<Instrument?> = listOf(
        null,
        Instruments.FLUTE,
        Instruments.CLARINET_BB,
        Instruments.SAX_ALTO,
        Instruments.TRUMPET_BB,
        Instruments.HORN_F,
        Instruments.VIOLIN,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AccentSage.copy(alpha = 0.12f),
                        PaperBg,
                        Color(0xFFF3EFE6),
                    ),
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CardBg,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color(0xFFE0DDD5)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when (step) {
                        0 -> "Bienvenue dans Fredonne"
                        1 -> "Votre instrument de départ"
                        else -> "Microphone (optionnel)"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkColor,
                )
                if (step == 0) {
                    Text(
                        text = "Un bloc-notes mélodique à une voix pour saisir vos idées au clavier virtuel, les écouter aussitôt et transposer sans effort.",
                        fontSize = 15.sp,
                        color = MutedInk,
                        lineHeight = 22.sp,
                    )
                    Text(
                        text = "100 % hors-ligne : aucune permission réseau, aucun compte, aucun traceur. Les règles de solfège vérifient mesures et tessiture sans jamais vous bloquer.",
                        fontSize = 14.sp,
                        color = MutedInk,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        androidx.compose.material3.Button(onClick = { step = 1 }) {
                            Text("Continuer")
                        }
                    }
                } else if (step == 1) {
                    Text(
                        text = "La portée affichera automatiquement l'écriture transposée pour cet instrument (modifiable à tout moment).",
                        fontSize = 14.sp,
                        color = MutedInk,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        quickInstruments.forEach { inst ->
                            val selected = chosenInstrument?.id == inst?.id
                            OutlinedButton(
                                onClick = { chosenInstrument = inst },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, if (selected) AccentSage else Color(0xFFD5D1C7)),
                            ) {
                                Text(
                                    text = inst?.let { instrumentLabel(it) } ?: "Concert (Ut, sons réels)",
                                    color = if (selected) AccentSage else InkColor,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (micGranted) onFinish(chosenInstrument, false)
                                else {
                                    chosenWithSample = false
                                    step = 2
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Portée vierge")
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                if (micGranted) onFinish(chosenInstrument, true)
                                else {
                                    chosenWithSample = true
                                    step = 2
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Avec exemple")
                        }
                    }
                } else {
                    Text(
                        text = "Pour fredonner une mélodie directement sur la portée et afficher l'accordeur chromatique, Fredonne a besoin d'accéder au microphone.",
                        fontSize = 15.sp,
                        color = MutedInk,
                        lineHeight = 22.sp,
                    )
                    Text(
                        text = "Tout le traitement audio reste 100 % sur votre appareil : rien n'est enregistré ni envoyé. Si vous refusez, l'accordeur et la dictée vocale resteront masqués et vous pourrez les activer plus tard dans À propos / Réglages.",
                        fontSize = 14.sp,
                        color = MutedInk,
                        lineHeight = 20.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onFinish(chosenInstrument, chosenWithSample) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Plus tard")
                        }
                        androidx.compose.material3.Button(
                            onClick = { onRequestMicAndFinish(chosenInstrument, chosenWithSample) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Autoriser le micro")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatsDialog(
    state: EditorState,
    exporter: ScoreExporter,
    onUpdateState: (EditorState) -> Unit,
    onDismiss: () -> Unit,
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }
    val importer = rememberScoreImporter(
        onFileLoaded = { file ->
            val isMidi = file.fileName.endsWith(".mid", ignoreCase = true) ||
                file.fileName.endsWith(".midi", ignoreCase = true) ||
                (file.bytes.size >= 4 &&
                    file.bytes[0] == 'M'.code.toByte() &&
                    file.bytes[1] == 'T'.code.toByte() &&
                    file.bytes[2] == 'h'.code.toByte() &&
                    file.bytes[3] == 'd'.code.toByte())
            runCatching {
                if (isMidi) {
                    state.importMidi(file.bytes)
                } else {
                    state.importShareCode(file.bytes.decodeToString())
                }
            }.onSuccess { updated ->
                onUpdateState(updated)
                statusMessage = "Fichier « ${file.fileName} » importé (${updated.melody.measures.size} mesures)"
            }.onFailure { err ->
                statusMessage = "Erreur d'import (${file.fileName}) : ${err.message ?: "format non reconnu"}"
            }
        },
        onError = { message ->
            statusMessage = message
        },
    )
    var shareCodeText by remember(state.melody, state.instrument, state.tempoBpm) {
        mutableStateOf(state.exportShareCode())
    }
    val qrChunks = remember(shareCodeText) {
        QrChunker.split(shareCodeText)
    }
    var qrPageIndex by remember(qrChunks) { mutableStateOf(0) }
    val currentChunk = qrChunks.getOrNull(qrPageIndex).orEmpty()
    val qrMatrix = remember(currentChunk) {
        if (currentChunk.isEmpty()) null else QrCodeEncoder.encodeOrNull(currentChunk)
    }
    var xmlText by remember(state.melody, state.instrument) {
        mutableStateOf(state.exportMusicXml())
    }

    if (showQrScanner) {
        QrScannerDialog(
            onPayloadAssembled = { payload ->
                showQrScanner = false
                shareCodeText = payload
                runCatching { state.importShareCode(payload) }
                    .onSuccess { updated ->
                        onUpdateState(updated)
                        statusMessage = "Partition importée depuis le QR code (${updated.melody.measures.size} mesures)"
                    }
                    .onFailure {
                        statusMessage = "QR lu mais format invalide : ${it.message ?: "format non reconnu"}"
                    }
            },
            onDismiss = { showQrScanner = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formats ouverts & QR Code hors-ligne") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Exportez la partition (avec transposition instrumentale et QR code de reprise sur le PDF) ou importez une partition hors-ligne.",
                    fontSize = 13.sp,
                    color = MutedInk,
                )
                OutlinedButton(
                    onClick = {
                        val xml = state.exportMusicXml()
                        xmlText = xml
                        val path = exporter.exportBytes(
                            fileName = "fredonne_partition.musicxml",
                            mimeType = "application/vnd.recordare.musicxml+xml",
                            bytes = xml.encodeToByteArray(),
                        )
                        statusMessage = "MusicXML 4.0 exporté ($path)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter en MusicXML (.musicxml)")
                }
                OutlinedButton(
                    onClick = {
                        val midi = state.exportMidi()
                        val path = exporter.exportBytes(
                            fileName = "fredonne_melodie.mid",
                            mimeType = "audio/midi",
                            bytes = midi,
                        )
                        statusMessage = "MIDI standard exporté (${midi.size} octets · $path)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter en MIDI (.mid)")
                }
                OutlinedButton(
                    onClick = {
                        val pdf = state.exportPdf()
                        val path = exporter.exportBytes(
                            fileName = "fredonne_partition.pdf",
                            mimeType = "application/pdf",
                            bytes = pdf,
                        )
                        statusMessage = "PDF A4 avec QR code ABC exporté (${pdf.size} octets · $path)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter la partition en PDF (.pdf)")
                }
                OutlinedButton(
                    onClick = {
                        val abc = state.exportAbc()
                        val path = exporter.exportBytes(
                            fileName = "fredonne_partition.abc",
                            mimeType = "text/vnd.abc",
                            bytes = abc.encodeToByteArray(),
                        )
                        statusMessage = "Notation ABC exportée ($path)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter en notation ABC (.abc)")
                }

                HorizontalDivider()
                Button(
                    onClick = { importer.pickFile() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSage),
                ) {
                    Text("Ouvrir un fichier (.abc, .musicxml, .mid)")
                }

                HorizontalDivider()
                Text("QR Code ABC & code de reprise rapide :", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Button(
                    onClick = { showQrScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSage),
                ) {
                    Text("Scanner un QR code (papier ou écran)")
                }
                if (qrMatrix != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(136.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFD8D5CC), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                        ) {
                            val cellW = size.width / qrMatrix.size
                            val cellH = size.height / qrMatrix.size
                            for (y in 0 until qrMatrix.size) {
                                for (x in 0 until qrMatrix.size) {
                                    if (qrMatrix[x, y]) {
                                        drawRect(
                                            color = InkColor,
                                            topLeft = Offset(x * cellW, y * cellH),
                                            size = Size(cellW + 0.5f, cellH + 0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (qrChunks.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { if (qrPageIndex > 0) qrPageIndex-- },
                                enabled = qrPageIndex > 0,
                            ) { Text("Précédent") }
                            Text(
                                text = "QR ${qrPageIndex + 1} / ${qrChunks.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedInk,
                            )
                            TextButton(
                                onClick = { if (qrPageIndex < qrChunks.lastIndex) qrPageIndex++ },
                                enabled = qrPageIndex < qrChunks.lastIndex,
                            ) { Text("Suivant") }
                        }
                    }
                } else {
                    Text(
                        text = "Partition trop dense pour un QR code. Utilisez l'export ABC ou MusicXML.",
                        fontSize = 12.sp,
                        color = MutedInk,
                    )
                }
                OutlinedTextField(
                    value = shareCodeText,
                    onValueChange = { shareCodeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                )
                OutlinedButton(
                    onClick = {
                        runCatching { state.importShareCode(shareCodeText) }
                            .onSuccess { updated ->
                                onUpdateState(updated)
                                statusMessage = "Partition importée (${updated.melody.measures.size} mesures)"
                            }
                            .onFailure { statusMessage = "Erreur code QR / ABC : ${it.message ?: "format invalide"}" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AccentSage),
                ) {
                    Text("Reprendre la partition (ABC ou FR1)", color = AccentSage)
                }

                HorizontalDivider()
                Text("Aperçu / Import MusicXML 4.0 :", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                OutlinedTextField(
                    value = xmlText,
                    onValueChange = { xmlText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                )
                OutlinedButton(
                    onClick = {
                        runCatching { state.importMusicXml(xmlText) }
                            .onSuccess { updated -> onUpdateState(updated) }
                            .onFailure { statusMessage = "Erreur MusicXML : ${it.message ?: "format non reconnu"}" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AccentSage),
                ) {
                    Text("Importer ce MusicXML sur la portée", color = AccentSage)
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = AccentSage,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun RehearsalDialog(
    state: EditorState,
    player: TonePlayer,
    onUpdateState: (EditorState) -> Unit,
    onDismiss: () -> Unit,
) {
    val tracker = remember { TapTempoTracker() }
    val clockStart = remember { TimeSource.Monotonic.markNow() }
    var metronomeRunning by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(-1) }

    LaunchedEffect(metronomeRunning, state.tempoBpm, state.melody.time) {
        if (!metronomeRunning) {
            currentBeat = -1
            return@LaunchedEffect
        }
        val beatsPerBar = state.melody.time.beats.coerceAtLeast(1)
        var beatIdx = 0
        while (metronomeRunning) {
            currentBeat = beatIdx % beatsPerBar
            val intervalMs = MetronomeEngine.beatIntervalMs(state.tempoBpm).roundToInt().toLong()
            delay(intervalMs)
            beatIdx++
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Outils de répétition") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Métronome & Tap Tempo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            val nowMs = clockStart.elapsedNow().inWholeMilliseconds
                            tracker.tap(nowMs)?.let { bpm -> onUpdateState(state.setTempo(bpm)) }
                        },
                    ) {
                        Text("Tap (${state.tempoBpm})")
                    }
                    FilterChip(
                        selected = metronomeRunning,
                        onClick = { metronomeRunning = !metronomeRunning },
                        label = { Text(if (metronomeRunning) "Arrêter" else "Métronome") },
                    )
                }
                if (metronomeRunning) {
                    val beatsPerBar = state.melody.time.beats.coerceAtLeast(1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (b in 0 until beatsPerBar) {
                            val active = b == currentBeat
                            val color = when {
                                active && b == 0 -> AccentSage
                                active -> CursorTeal
                                else -> Color(0xFFE0DDD5)
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (b == 0) 16.dp else 12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color),
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text("Boucle A–B & Vitesse", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.loopEnabled,
                        onClick = { onUpdateState(state.toggleLoop()) },
                        label = { Text(if (state.loopEnabled) "Boucle active" else "Activer boucle") },
                    )
                    if (state.loopEnabled && state.melody.measures.isNotEmpty()) {
                        val maxIdx = state.melody.measures.lastIndex
                        Text(
                            "M${state.loopStartMeasure + 1}–M${state.loopEndMeasure + 1}",
                            fontSize = 13.sp,
                            color = AccentSage,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(
                            onClick = {
                                val nextStart = (state.loopStartMeasure + 1).coerceAtMost(state.loopEndMeasure)
                                onUpdateState(state.setLoopRange(nextStart, state.loopEndMeasure))
                            },
                            enabled = state.loopStartMeasure < state.loopEndMeasure,
                        ) { Text("A+") }
                        TextButton(
                            onClick = {
                                val prevStart = (state.loopStartMeasure - 1).coerceAtLeast(0)
                                onUpdateState(state.setLoopRange(prevStart, state.loopEndMeasure))
                            },
                            enabled = state.loopStartMeasure > 0,
                        ) { Text("A-") }
                        TextButton(
                            onClick = {
                                val nextEnd = (state.loopEndMeasure + 1).coerceAtMost(maxIdx)
                                onUpdateState(state.setLoopRange(state.loopStartMeasure, nextEnd))
                            },
                            enabled = state.loopEndMeasure < maxIdx,
                        ) { Text("B+") }
                        TextButton(
                            onClick = {
                                val prevEnd = (state.loopEndMeasure - 1).coerceAtLeast(state.loopStartMeasure)
                                onUpdateState(state.setLoopRange(state.loopStartMeasure, prevEnd))
                            },
                            enabled = state.loopEndMeasure > state.loopStartMeasure,
                        ) { Text("B-") }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PracticeLoop.PRESET_SPEEDS.forEach { spd ->
                        FilterChip(
                            selected = state.playbackSpeed == spd,
                            onClick = { onUpdateState(state.setPlaybackSpeed(spd)) },
                            label = { Text("${spd}×") },
                        )
                    }
                }

                HorizontalDivider()

                Text("Diapason (référence La4)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(440.0, 442.0).forEach { hz ->
                        FilterChip(
                            selected = state.tunerReferenceA4Hz == hz,
                            onClick = { onUpdateState(state.setTunerReferenceA4Hz(hz)) },
                            label = { Text("${hz.toInt()} Hz") },
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            player.playSingleNote(
                                midi = 69,
                                durationMs = 1100,
                                timbre = InstrumentTimbre.FLUTE,
                                referenceA4Hz = state.tunerReferenceA4Hz,
                            )
                        },
                    ) {
                        Text("Écouter La4")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun LyricAndBreathDialog(
    note: Note,
    onSave: (lyric: String?, breathMark: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var lyricText by remember(note) { mutableStateOf(note.lyric.orEmpty()) }
    var breath by remember(note) { mutableStateOf(note.breathMark) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parole & respiration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = lyricText,
                    onValueChange = { lyricText = it },
                    label = { Text("Syllabe sous la note") },
                    singleLine = true,
                )
                FilterChip(
                    selected = breath,
                    onClick = { breath = !breath },
                    label = { Text(if (breath) "Respiration (') activée" else "Ajouter une respiration (')") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(lyricText, breath) }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@Composable
private fun TunerDialog(
    referenceA4Hz: Double,
    onDismiss: () -> Unit,
) {
    val tuner = remember(referenceA4Hz) { ChromaticTuner(referenceA4Hz = referenceA4Hz) }
    var reading by remember { mutableStateOf<TunerReading?>(null) }

    DisposableEffect(tuner) {
        val session = createLiveMicSession()
        val ring = FloatArray(2048)
        var filled = 0
        session?.start(16_000) { chunk ->
            for (s in chunk) {
                if (filled < ring.size) {
                    ring[filled++] = s
                } else {
                    ring.copyInto(ring, destinationOffset = 0, startIndex = 1)
                    ring[ring.lastIndex] = s
                }
            }
            if (filled >= 1024) {
                reading = tuner.analyzeSamples(ring.copyOf(filled), 16_000)
            }
        }
        onDispose { session?.stop() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accordeur (La = ${referenceA4Hz.toInt()} Hz)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val r = reading
                if (r == null) {
                    Text("Jouez ou fredonnez une note…", color = MutedInk)
                } else {
                    val noteName = frenchNoteName(r.nearestPitch.step) + when (r.nearestPitch.alter) {
                        1 -> "#"
                        -1 -> "b"
                        else -> ""
                    } + r.nearestPitch.octave
                    Text(noteName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AccentSage)
                    val cents = kotlin.math.round(r.centsOffset).toInt()
                    val statusLabel = when (r.status) {
                        TuningStatus.IN_TUNE -> "Juste"
                        TuningStatus.FLAT -> "Trop bas ($cents cents)"
                        TuningStatus.SHARP -> "Trop haut (+$cents cents)"
                    }
                    Text(statusLabel, fontWeight = FontWeight.SemiBold)
                    Text("${kotlin.math.round(r.frequencyHz * 10) / 10.0} Hz", fontSize = 13.sp, color = MutedInk)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun HumDialog(
    state: EditorState,
    onApplyMelody: (Melody) -> Unit,
    onDismiss: () -> Unit,
) {
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Appuyez sur Enregistrer puis fredonnez une courte phrase.") }
    var busy by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<HumCapture?>(null) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<LiveMicSession?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            session?.stop()
            capture?.cancel()
        }
    }

    AlertDialog(
        onDismissRequest = {
            session?.stop()
            onDismiss()
        },
        title = { Text("Fredonner une mélodie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(status, fontSize = 14.sp, color = MutedInk)
                androidx.compose.material3.Button(
                    enabled = !busy,
                    onClick = {
                        if (!recording) {
                            val mic = createLiveMicSession()
                            if (mic == null) {
                                status = "Capture micro indisponible sur cet appareil."
                                return@Button
                            }
                            session = mic
                            val current = HumCapture(scope)
                            capture = current
                            recording = true
                            status = "Enregistrement en cours… Fredonnez puis appuyez sur Terminer."
                            mic.start(16_000) { chunk -> current.offer(chunk) }
                        } else {
                            recording = false
                            session?.stop()
                            session = null
                            val finished = capture
                            capture = null
                            busy = true
                            status = "Transcription en cours…"
                            scope.launch {
                                val samples = finished?.finish() ?: FloatArray(0)
                                if (samples.size < 4000) {
                                    status = "Enregistrement trop court, réessayez."
                                    busy = false
                                } else {
                                    val transcribed = withContext(Dispatchers.Default) {
                                        AudioMelodyTranscriber(
                                            fr.fredonne.core.music.AudioPitchDetector(sampleRate = 16_000),
                                        ).transcribePcm(
                                            pcm = samples,
                                            bpm = state.tempoBpm,
                                            time = state.melody.time,
                                            key = state.melody.key,
                                        )
                                    }
                                    busy = false
                                    onApplyMelody(transcribed)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (recording) "Terminer et transcrire" else "Enregistrer")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    session?.stop()
                    onDismiss()
                },
            ) { Text("Annuler") }
        },
    )
}

@Composable
private fun SettingsDialog(
    referenceA4Hz: Double,
    onChangeReferenceA4: (Double) -> Unit,
    micPermission: MicPermissionState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réglages") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Microphone & Accordeur", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkColor)
                if (micPermission.granted) {
                    Text(
                        "Microphone autorisé : l'accordeur et la dictée « Fredonner » sont actifs dans la barre d'outils.",
                        fontSize = 13.sp,
                        color = MutedInk,
                    )
                } else {
                    Text(
                        "Microphone non autorisé : l'accordeur et la dictée vocale demanderont l'autorisation à leur premier usage.",
                        fontSize = 13.sp,
                        color = MutedInk,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(onClick = micPermission.request) {
                            Text("Autoriser le micro")
                        }
                        OutlinedButton(onClick = micPermission.openSettings) {
                            Text("Réglages système")
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFECE9E2))
                Text("Diapason (La4)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(440.0, 442.0).forEach { hz ->
                        FilterChip(
                            selected = kotlin.math.abs(referenceA4Hz - hz) < 0.5,
                            onClick = { onChangeReferenceA4(hz) },
                            label = { Text("${hz.toInt()} Hz") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            PilmilLegalSection(
                appName = "Fredonne",
                appSummary = "Bloc-notes mélodique à une voix, 100 % hors-ligne.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}

private fun frenchNoteName(step: Step): String = when (step) {
    Step.C -> "Do"
    Step.D -> "Ré"
    Step.E -> "Mi"
    Step.F -> "Fa"
    Step.G -> "Sol"
    Step.A -> "La"
    Step.B -> "Si"
}

private fun formatKeyLabel(key: Key): String {
    val alter = when {
        key.tonicAlter > 0 -> "#"
        key.tonicAlter < 0 -> "b"
        else -> ""
    }
    val mode = if (key.mode == Mode.MAJOR) "maj" else "min"
    return "${frenchNoteName(key.tonicStep)}$alter $mode"
}

private fun instrumentLabel(instrument: Instrument): String = when (instrument.id) {
    "flute" -> "Flûte (Ut)"
    "oboe" -> "Hautbois (Ut)"
    "clarinet_bb" -> "Clarinette (Sib)"
    "sax_soprano" -> "Saxophone soprano (Sib)"
    "sax_alto" -> "Saxophone alto (Mib)"
    "sax_tenor" -> "Saxophone ténor (Sib)"
    "sax_baritone" -> "Saxophone baryton (Mib)"
    "trumpet_bb" -> "Trompette (Sib)"
    "horn_f" -> "Cor (Fa)"
    "trombone" -> "Trombone (Ut, Clé de Fa)"
    "violin" -> "Violon (Ut)"
    "harp_celtic" -> "Harpe celtique (Ut, diatonique)"
    "harp_pedal" -> "Harpe à pédales (Concert, 47 cordes)"
    "guitar" -> "Guitare (Ut, 8va)"
    "bass_electric" -> "Basse électrique (Clé de Fa, 8vb)"
    "contrabass" -> "Contrebasse (Clé de Fa, 8vb)"
    "voice_soprano" -> "Voix Soprano"
    "voice_alto" -> "Voix Alto"
    "voice_tenor" -> "Voix Ténor (Sol 8vb)"
    "voice_bass" -> "Voix Basse (Clé de Fa)"
    else -> instrument.id
}
