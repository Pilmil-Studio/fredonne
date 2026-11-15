# Fredonne

Application libre pour Android (portage iOS en préparation ; Kotlin Multiplatform / Compose Multiplatform) pour écrire une mélodie sur son téléphone, l'écouter avec un timbre acoustique réaliste et la transposer dans une autre tonalité ou pour un instrument transpositeur. Tout reste sur l'appareil.

## Principes

- Gratuite et libre (MPL-2.0)
- Aucune permission Internet, aucun traceur, aucun compte
- 100 % hors-ligne : les partitions restent sur le téléphone
- **Formats ouverts** : MusicXML 4.0 (`.musicxml`), Standard MIDI File (`.mid`), ABC (`.abc`), partition PDF vectorielle (`.pdf`)

## Fonctionnalités implémentées

- **Saisie mélodique fluide** : curseur d'insertion, rangée de durées (ronde à double-croche, pointée, silence), mini clavier de piano défilable par octave et **transcription au micro** (`AudioPitchDetector`, `AudioMelodyTranscriber`).
- **Synthèse acoustique hybride à 44,1 kHz (`ToneSynthesizer`, `AcousticSampleBank`)** :
  - 6 familles de timbres (`WARM_KEYS`, `FLUTE`, `REED`, `BRASS`, `STRING`, `BASS`) combinant **échantillons acoustiques CC0** (VSCO 2 CE encodés en loi μ 16 kHz) et synthèse harmonique sans vibrato artificiel, avec partiels inharmoniques ($f_n = n f_0 \sqrt{1 + B n^2}$, Fletcher 1964) et pondération d'isosonie **ISO 226:2023**.
- **Instruments, clés et transposition (`Instruments`, `MusicTheory`, `HarpPedals`)** :
  - 20 instruments (bois, cuivres, cordes, guitare, basse électrique, contrebasse, voix SATB, **harpe celtique** avec contrôle diatonique des leviers, **harpe à pédales 47 cordes** avec suivi des 7 pédales), instruments transpositeurs (Si♭, Mi♭, Fa, Ut, Guitare 8va) et **5 clés** (Sol, Sol octaviée, Fa, Ut 3e et Ut 4e).
  - Résolution enharmonique contextuelle inspirée de **PS13 (Meredith, 2006)** et simplification automatique des armures théoriques dépassant 6 altérations.
  - Détection automatique de la tonalité par corrélation de Pearson sur les profils cognitifs de **Krumhansl & Schmuckler (1990)**.
- **Gravure & vérificateur en toile de fond (`Verifier`, `StaffCanvas`)** :
  - Espacement horizontal non linéaire des durées selon la loi logarithmique de **Gourlay (1987)** et règles de hampes d'Elaine Gould.
  - Vérification non bloquante des durées de mesure en fractions exactes (`Fraction`), de la tessiture réelle de l'instrument, des leviers/pédales de harpe et du contrepoint à deux voix (quintes et octaves parallèles, croisements de voix selon **Fux / Tymoczko**, et rugosité sensorielle en bande critique selon **Plomp & Levelt 1965 / Sethares 1993**).
- **Outils de répétition** : métronome (`MetronomeEngine`), boucle de travail (`PracticeLoop`) et accordeur chromatique (`ChromaticTuner`, `MicTunerBridge`).
- **Import / Export & partage hors-ligne (`MusicXmlCodec`, `MidiCodec`, `AbcCodec`, `ScorePdfGenerator`, `ShareCodec`)** :
  - Import et export **MusicXML 4.0** (`<score-partwise version="4.0">` avec `<transpose>` pour les instruments transpositeurs).
  - Import et export **MIDI standard** (`.mid`, SMF Format 0) et **ABC** (`.abc`).
  - Export **PDF A4 vectoriel** sans filigrane.
  - Partage hors-ligne de partitions entre appareils par **QR Code** (`QrCodeEncoder`, `QrChunker`, `QrScanner`).
  - Annuler et rétablir multi-niveaux.

## Architecture

| Module | Rôle |
|---|---|
| `core:music` | Modèle musical en fractions exactes (`Fraction`), 20 instruments et harpes (`Instruments.kt`, `HarpPedals.kt`), transposition, orthographe enharmonique PS13 & détection tonale Krumhansl–Schmuckler (`Theory.kt`), vérificateur (`Verifier.kt`), métronome/boucle/accordeur, codecs `MusicXmlCodec`, `MidiCodec`, `AbcCodec`, `ShareCodec` et `ScorePdfGenerator`. Kotlin Multiplatform pur, couvert par des tests unitaires. |
| `shared` | Interface Compose Multiplatform (`FredonneApp.kt`), état de l'éditeur (`EditorState.kt`), synthétiseur hybride (`ToneSynthesizer.kt`, `AcousticSampleBank.kt`), lecteur audio (`TonePlayer`), micro/QR (`MicTunerBridge`, `QrScanner`), identité Pilmil Studio et exporteur de fichiers (`ScoreExporter`) pour Android et iOS. |
| `app` | Application Android (`MainActivity.kt`) intégrant `pilmil-core` (`core-platform`) et tests unitaires / captures. |
| `iosApp` | Coque iOS / Xcode (`FredonneApp.swift`). |

Détails et choix de conception dans [`docs/conception.md`](docs/conception.md).

## Contribuer

Les idées, les retours d'utilisation et les corrections sont bienvenus dans les tickets. Contact : pilmil.studio@proton.me

## Licence

[MPL-2.0](LICENSE). Une application de Pilmil Studio.
