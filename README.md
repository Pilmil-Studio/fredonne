# Fredonne

Fredonne sert à noter une mélodie sur son téléphone, à l'écouter et à la transposer, par exemple pour un instrument en si bémol, en mi bémol ou en fa. Il s'adresse aux choristes et aux musiciens d'harmonie ou d'orchestre qui veulent garder un thème, l'apprendre ou le partager. Tout reste sur l'appareil : pas de compte, pas de publicité, pas d'accès à Internet.

- Site : <https://pilmil-studio.github.io/fredonne/>
- Téléchargement (Android) : <https://github.com/Pilmil-Studio/fredonne/releases/latest>

<p>
  <img src="docs/captures/editeur.png" alt="L'éditeur de mélodie" width="240">
  <img src="docs/captures/transpositeur.png" alt="La transposition pour un instrument" width="240">
</p>

Niveau actuel : prototype. L'application tourne sur Android ; le portage iOS est en préparation (la coque existe mais n'est pas aboutie) et l'interface est en français uniquement.

## Ce que fait l'application

- **Éditeur à une voix** : notes, silences, altérations, notes pointées, tonalité, chiffrage de mesure, tempo, paroles sous les notes, repères de respiration, annuler et rétablir (50 étapes).
- **Enregistrement automatique** : la partition en cours est écrite dans le stockage privé de l'appli peu après chaque modification et à la mise en arrière-plan, puis reprise au lancement. Un fichier illisible est mis de côté (renommé, pas supprimé) et l'appli repart d'une partition vide.
- **Écoute** avec timbres acoustiques, boucle sur quelques mesures et vitesse de lecture réglable.
- **Transposition** en demi-tons ou vers une tonalité, 20 instruments (dont les transpositeurs en si bémol, mi bémol et fa, deux harpes) et vérification de la tessiture.
- **Métronome** et tap tempo.
- **Accordeur chromatique** (diapason 440 ou 442 Hz).
- **Saisie en fredonnant** : transcription de la voix en notes sur la portée.
- **Notation ABC** : export et reprise.
- **QR Code de partage** : une courte mélodie se transmet par QR Code, affiché ou imprimé sur le PDF, et se lit avec l'appareil photo.
- **Exports** : MusicXML 4.0, MIDI, PDF A4 vectoriel et ABC. Import par collage de texte (MusicXML, ABC ou code Fredonne).

## Permissions et vie privée

| Permission | Pourquoi |
|---|---|
| Internet | Aucune : l'application ne la déclare pas. |
| Micro (`RECORD_AUDIO`) | Accordeur et saisie en fredonnant seulement. Le son est analysé en mémoire sur le téléphone, jamais écrit sur disque. Demande facultative. |
| Appareil photo (`CAMERA`) | Lecture d'un QR Code de partition seulement. L'image est analysée en mémoire, jamais enregistrée. |

Politique complète : <https://pilmil-studio.github.io/fredonne/confidentialite.html>

## Partie technique

Kotlin Multiplatform et Compose Multiplatform. Le moteur musical (`core:music`) est du Kotlin pur, couvert par des tests unitaires ; l'interface et l'état de l'éditeur sont dans `shared`.

| Module | Rôle |
|---|---|
| `core:music` | Modèle musical en fractions exactes (`Fraction`), instruments et harpes, transposition, orthographe enharmonique, détection de tonalité, vérificateur, métronome, tempo et modulation métrique (`Tempo`, `MetricModulation`), figures de notes (`NoteFigure`), boucle, accordeur, détection de hauteur et transcription audio, codecs `MusicXmlCodec`, `MidiCodec`, `AbcCodec`, `ShareCodec`, `QrChunker` et `ScorePdfGenerator`. |
| `core:engraving` | Mise en page de la portée à partir du modèle : ce qu'il faut dessiner et où. Kotlin pur, couvert par des tests. |
| `shared` | Interface Compose (`FredonneApp.kt`), état de l'éditeur (`EditorState.kt`), reprise de session (`EditorSessionCodec.kt`, `SessionStore`), synthétiseur (`ToneSynthesizer.kt`, `AcousticSampleBank.kt`), lecteur audio, micro et QR (`MicTunerBridge`, `QrScanner`), export de fichiers, pour Android et iOS. |
| `app` | Application Android (`MainActivity.kt`), intégrant `pilmil-core` (sous-module git), tests unitaires et captures. |
| `iosApp` | Coque iOS / Xcode, non aboutie. |

Construire et tester (JDK 17, sous-module à récupérer avec `git submodule update --init`) :

```
./gradlew :core:music:jvmTest :shared:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleDebug
```

Détails et choix de conception dans [`docs/conception.md`](docs/conception.md) et [`documentation/`](documentation/).

## Contribuer

Les idées, les retours d'utilisation et les corrections sont bienvenus dans les tickets. Contact : pilmil.studio@proton.me

## Licence

[MPL-2.0](LICENSE). Une application de Pilmil Studio.

## Fondements

Références sur lesquelles reposent certains choix du moteur :

- Synthèse : partiels inharmoniques $f_n = n f_0 \sqrt{1 + B n^2}$ (Fletcher, 1964), pondération d'isosonie ISO 226:2023, échantillons acoustiques CC0 (VSCO 2 CE) encodés en loi μ 16 kHz.
- Orthographe : résolution enharmonique contextuelle inspirée de PS13 (Meredith, 2006) ; simplification des armures au-delà de 6 altérations.
- Tonalité : corrélation de Pearson sur les profils de Krumhansl et Schmuckler (1990).
- Gravure : espacement logarithmique des durées (Gourlay, 1987), règles de hampes d'Elaine Gould.
- Vérification à deux voix : quintes et octaves parallèles, croisements de voix (Fux, Tymoczko), rugosité en bande critique (Plomp et Levelt, 1965 ; Sethares, 1993).
- Détection de hauteur : fonction de différence moyenne cumulée normalisée (YIN).
