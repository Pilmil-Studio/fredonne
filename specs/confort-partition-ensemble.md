# Spécification & Cahier des charges — Confort de partition et jeu en ensemble (#2)

> **État** : Spécification rédigée avant implémentation dans `:core:music`.  
> **Existant vérifié** : `ScorePdfGenerator` (export PDF A4 vectoriel) et `ToneSynthesizer` (timbres acoustiques + `MelodyEventTiming` pour curseur de lecture) existent déjà dans le projet. Ce document couvre les briques manquantes dans `:core:music`.

---

## 1. Besoin et objectifs
Compléter le modèle musical et les codecs de **Fredonne** pour les usages en chorale, orchestre d'harmonie et musique d'ensemble :
1. **Clés supplémentaires** : Clé d'Ut 3e ligne (`ALTO`) et Clé d'Ut 4e ligne (`TENOR`) en complément des clés existantes (`TREBLE`, `TREBLE_8VB`, `BASS`).
2. **Paroles sous les notes** : 1 ligne de texte syllabique simple par note (`lyric: String?`) pour les choristes.
3. **Repères de respiration** : indicateur de respiration (`breathMark: Boolean`) entre deux phrases musicales.
4. **Rendu & export cohérents** : prise en charge dans `MusicXmlCodec` (import/export) et `ScorePdfGenerator` (affichage clé d'Ut, paroles sous la portée, signe de respiration `'`).

---

## 2. Cahier des charges technique

### 2.1 Extension de `Clef` (`Instruments.kt`)
- Ajouter à `enum class Clef` :
  - `ALTO(sign = "C", line = 3, octaveChange = 0, bottomLinePitch = Pitch(Step.F, 3), displaySymbol = "C3")` — ligne centrale = `Do4`, donc ligne du bas = `Fa3`.
  - `TENOR(sign = "C", line = 4, octaveChange = 0, bottomLinePitch = Pitch(Step.D, 3), displaySymbol = "C4")` — 4e ligne = `Do4`, donc ligne du bas = `Ré3`.
- Conserver `TREBLE`, `TREBLE_8VB`, `BASS` inchangés.

### 2.2 Extension de `Note` (`Melody.kt`)
- Ajouter deux propriétés optionnelles avec valeurs par défaut (rétrocompatible) :
  - `val lyric: String? = null`
  - `val breathMark: Boolean = false`
- `Melody.transposed(...)` et `Melody.respellPractical(...)` doivent **préserver** `lyric` et `breathMark`.

### 2.3 Extension de `MusicXmlCodec`
- **Export** :
  - `<clef>` : gère `ALTO` (`<sign>C</sign><line>3</line>`) et `TENOR` (`<sign>C</sign><line>4</line>`).
  - `<lyric number="1"><text>...</text></lyric>` si `note.lyric != null && note.lyric.isNotBlank()` (avec échappement XML).
  - `<notations><articulations><breath-mark/></articulations></notations>` si `note.breathMark == true`.
- **Import** :
  - Lecture de `<lyric><text>...</text></lyric>` dans `Note.lyric`.
  - Lecture de `<breath-mark` dans `Note.breathMark`.

### 2.4 Extension de `ScorePdfGenerator`
- Affichage des clés `ALTO` et `TENOR` (ou indication claire de clé d'Ut 3 / Ut 4 sans casser le placement diatonique calculé via `clef.bottomLinePitch`).
- Affichage centré de `note.lyric` sous la portée (sans chevaucher la portée suivante).
- Affichage discret du repère de respiration (`'`) au-dessus de la ligne supérieure à droite de la note lorsque `note.breathMark == true`.

---

## 3. Critères d'acceptation & tests unitaires
- `Clef.ALTO.bottomLinePitch == Pitch(Step.F, 3)` et `Clef.TENOR.bottomLinePitch == Pitch(Step.D, 3)`.
- `Melody.transposed(...)` conserve `lyric` et `breathMark` sur chaque note.
- Aller-retour `MusicXmlCodec.encode` → `MusicXmlCodec.decode` préserve `lyric` et `breathMark`.
- `ScorePdfGenerator.generate(...)` produit un PDF valide (`%PDF-1.4`) avec `Clef.ALTO`, `Clef.TENOR`, des paroles et un repère de respiration sans exception.
