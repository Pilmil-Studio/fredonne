# Guide utilisateur — Fredonne

Fredonne est un éditeur de mélodie à une voix avec lecture audio immédiate, transposition et export en formats ouverts (MusicXML, MIDI, PDF), fonctionnant 100 % hors-ligne.

---

## 1. Premier lancement (`OnboardingScreen`)

Un assistant en **3 étapes** prépare l'espace de travail :
1. Présentation du fonctionnement hors-ligne.
2. Choix de l'instrument de départ et démarrage soit sur une portée vierge, soit avec l'exemple *Frère Jacques*.
3. **Microphone (optionnel)** : explication locale avant la demande système (utilisé uniquement pour l'accordeur chromatique et la dictée « Fredonner », sans enregistrement ni envoi). Si la permission est refusée, ces deux options restent masquées dans l'éditeur et un bouton **« Autoriser le micro »** reste disponible dans **À propos / Réglages**.

---

## 2. Interface principale d'édition

L'application s'ouvre directement sur l'éditeur de partition (sans barre d'onglets inférieure) :

### Barre supérieure (`TopAppBar`)
- **Lecture / Arrêt** : écoute immédiate de la mélodie synthétisée localement.
- **Annuler / Rétablir** : historique jusqu'à **50 états**.
- **À propos** (`AboutDialog`) : informations sur l'application et sa licence.

### Ruban d'actions rapides
- **Tonalité** (`KeyAndTransposeDialog`) : changement de tonalité, transposition chromatique ou diatonique, et **estimation automatique de la tonalité** (algorithme de Krumhansl-Schmuckler).
- **Instrument** (`InstrumentDialog`) : sélection parmi **20 instruments** (y compris instruments transpositeurs en Si♭, Mi♭, Fa).
- **MusicXML · MIDI · PDF** (`FormatsDialog`) : import/export aux formats standards et génération d'un **QR Code vectoriel** (`FR1|...`) pour partager une courte mélodie hors-ligne.
- **Répéter** (`RehearsalDialog`) : outil de travail avec métronome, *tap tempo*, boucle **A-B**, vitesse de lecture réglable (**0,5x à 1,0x**) et diapason (**440 Hz ou 442 Hz**).
- **Parole · Resp.** (`LyricAndBreathDialog`, contextuel) : ajout de paroles sous les notes ou de repères de respiration.
- **Exemple** / **Effacer** : chargement rapide d'un exemple ou remise à zéro de la portée.

### Portée interactive et saisie (`StaffCanvas`, `VerifierBar`, `DurationToolbar`, `PianoKeyboard`)
- **Portée interactive (`StaffCanvas`)** : retour à la ligne automatique des systèmes, prise en charge des clés de Sol, Sol octave basse (8vb), Fa, Ut 3 et Ut 4.
- **Barre de vérification (`VerifierBar`)** : signale en temps réel les mesures incomplètes, les notes hors tessiture de l'instrument, les levées/pédales de harpe et les quintes ou octaves parallèles.
- **Contrôles de tempo et curseur** : réglage du tempo (**50 à 160 BPM**), déplacement du curseur et suppression de note.
- **Barre de durées (`DurationToolbar`)** : de la **Ronde** à la **Double-croche**, note **Pointée**, **Silence**, et bouton **+ Mesure**.
- **Clavier virtuel (`PianoKeyboard`)** : clavier sur 2 octaves avec décalage **-1 / +1 octave**.
