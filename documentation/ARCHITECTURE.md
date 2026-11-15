# Architecture & Choix Techniques — Fredonne

## 1. Structure du projet et modules

Fredonne est structuré en **Kotlin Multiplatform (KMP)** :
- `:core:music` : moteur musical pur Kotlin (JVM + iOS) sans dépendance UI : modèles de portée/mesures, codecs `MusicXmlCodec`, `MidiCodec`, générateur PDF `ScorePdfGenerator`, encodeur QR `QrCodeEncoder`, détection de hauteur YIN (`AudioPitchDetector`) et transcription (`AudioMelodyTranscriber`).
- `:shared` : interface Compose Multiplatform (`StaffCanvas`, `PianoKeyboard`, modales) et synthétiseur PCM + contrats `expect/actual` audio.
- `:app` (`fr.fredonne.app`, v1.0.5) : hôte Android (`AudioTrack`, SAF pour import/export).
- `pilmil-core` : socle partagé.

---

## 2. Manifeste, permissions et confidentialité

- **Permissions déclarées** : `CAMERA` (`required="false"`, scan QR Code) et `RECORD_AUDIO` (`hardware.microphone` `required="false"`, accordeur `ChromaticTuner` et dictée `AudioMelodyTranscriber` sur Android ; demandée uniquement après explication dans l'onboarding ou depuis À propos/Réglages). Aucune permission `INTERNET`.
- **`android:allowBackup="false"`**.

---

## 3. Choix techniques vérifiés dans le code

### Synthèse audio hybride hors-ligne
- Génération PCM **44,1 kHz, 16 bits, mono** via `ToneSynthesizer` combiné à des échantillons acoustiques CC0 (VSCO2 encodés en µ-law dans `AcousticSampleBank`).
- Rééchantillonnage par interpolation d'**Hermite à 4 points**, compensation physiologique selon les courbes **ISO 226:2023**, saturation douce (`tanh`) et réverbération à 2 étages passe-tout.
- Restitution native via `AudioTrack` sur Android et `AVAudioPlayer` sur iOS.

### Codecs et exports sans dépendances tierces
- **`MusicXmlCodec`** : parseur et sérialiseur MusicXML en Kotlin pur.
- **`MidiCodec`** : lecture/écriture Standard MIDI File (SMF format 0) avec orthographe enharmonique calculée par l'algorithme **PS13 de Meredith**.
- **`ScorePdfGenerator`** : génération directe de fichiers **PDF 1.4** en Kotlin pur (sans bibliothèque PDF externe).
- **`QrCodeEncoder`** : génération vectorielle complète (arithmétique de corps fini **GF(2⁸)**) au format compact `FR1|...`.
- **Estimation de tonalité** : algorithme de corrélation de profils de **Krumhansl-Schmuckler**.
