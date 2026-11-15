# Spécification & Cahier des charges — Outils de répétition (#1)

> **État** : Spécification rédigée avant implémentation dans `:core:music`.  
> **Principes** : 100 % hors-ligne, zéro dépendance réseau, réutilisation directe des briques audio existantes (`AudioPitchDetector`).

---

## 1. Besoin et objectifs
Donner aux musiciens d'ensemble (harmonie, chorale, fanfare) et aux amateurs les trois outils de répétition quotidiens directement dans **Fredonne**, sans avoir à installer d'autre application :
1. **Accordeur chromatique et diapason** (`La 440 Hz` / `La 442 Hz` / plage `430–450 Hz`),
2. **Métronome, tap tempo et décompte** avant capture micro,
3. **Boucle A–B et variation de tempo** à la lecture (`0.5×` à `1.25×` sans changer la hauteur).

---

## 2. Cahier des charges fonctionnel

### 2.1 Accordeur chromatique & diapason (`ChromaticTuner`)
- **Entrée** : buffer PCM mono (ou fréquence détectée via `AudioPitchDetector`) + fréquence de référence `referenceA4Hz` (par défaut `440.0`, preset rapide `442.0` pour orchestre/harmonie, borné à `415.0..466.0` Hz).
- **Sortie (`TunerReading`)** :
  - `frequencyHz: Double`
  - `nearestPitch: Pitch` (note chromatique la plus proche)
  - `centsOffset: Double` (écart en cents dans `[-50.0, +50.0]`)
  - `status: TuningStatus` (`FLAT` si `< -thresholdCents`, `IN_TUNE` si `|cents| <= thresholdCents`, `SHARP` si `> +thresholdCents`, seuil par défaut `5.0` cents)
  - `confidence: Double`
- **Diapason** : calcul de la fréquence exacte d'une note cible selon `referenceA4Hz` (`f = referenceA4Hz * 2^((midi - 69) / 12)`).

### 2.2 Métronome, Tap Tempo & Décompte (`MetronomeEngine`)
- **Métronome** :
  - Tempo borné à `30..240` BPM.
  - Génération d'une grille de battements (`MetronomeBeat(beatIndex, isAccent, timestampMs)`) respectant `TimeSignature` (temps 1 accentué, temps suivants normaux ; pour `6/8`, possibilité de battre à la noire pointée ou à la croche).
  - Génération d'un **décompte** (`countInBeats(timeSignature, bars = 1)`) avant le début d'une capture micro.
- **Tap Tempo (`TapTempoTracker`)** :
  - Enregistrement des horodatages `tap(nowMs)`.
  - Remise à zéro automatique si l'intervalle dépasse `2500 ms`.
  - Calcul de la moyenne glissante sur les derniers intervalles valides (minimum 2 taps, fenêtre jusqu'à 8 taps), bornée à `30..240` BPM.

### 2.3 Boucle A–B & ralentisseur de tempo (`PracticeLoop`)
- **Sélection de boucle** : `startMeasure`..`endMeasure` (inclus, validés dans `0..measures.lastIndex` avec `startMeasure <= endMeasure`).
- **Facteur de vitesse** : `speedFactor` dans `0.25..2.0` (presets : `0.5`, `0.6`, `0.8`, `1.0`).
- **Tempo effectif** : `effectiveBpm = (baseTempoBpm * speedFactor).roundToInt().coerceIn(20, 300)`.
- **Extraction** : `sliceMelody(melody)` retourne une `Melody` ne contenant que les mesures de la boucle pour la lecture répétée en boucle, sans modifier la partition originale.

---

## 3. Critères d'acceptation & tests unitaires
- `ChromaticTuner` identifie `La 440.0 Hz` comme `A4` à `0.0 cents` (`IN_TUNE`) avec référence `440 Hz`, et comme `FLAT` (`~ -7.85 cents`) avec référence `442 Hz`.
- `ChromaticTuner.analyzeSamples(...)` sur une sinusoïde synthétique (ex. `Si♭3` / `A4`) retourne la note attendue à moins de `5 cents`.
- `TapTempoTracker` converge vers `120 BPM` pour des taps espacés de `500 ms` et réinitialise après une pause `> 2500 ms`.
- `MetronomeEngine` génère le bon nombre de temps et d'accents pour `4/4`, `3/4`, `6/8`.
- `PracticeLoop` valide les bornes de mesures, applique le ralentissement sans altérer les hauteurs, et extrait exactement la plage `[startMeasure..endMeasure]`.
