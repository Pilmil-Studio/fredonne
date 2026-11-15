# Spécification technique — Saisie mélodique au microphone (`fredonne`)

> Statut : Spécification de référence (15 novembre 2026).
> Objectif : Permettre l'écriture directe sur la portée en chantant, fredonnant, sifflant ou jouant d'un instrument monodique (flûte, clarinette, saxophone, trompette, violon, piano/guitare note à note), **100 % hors-ligne**.

---

## 1. Principes et périmètre

- **Monophonique uniquement** : conforme au modèle de données actuel de Fredonne (mélodie à une voix). La transcription polyphonique (accords) est volontairement hors périmètre afin de garder un moteur léger, rapide sur CPU et fiable sans serveur.
- **Zéro réseau, zéro enregistrement conservé** : l'audio capté via `AudioRecord` (Android) / `AVAudioEngine` (iOS) est traité à la volée en mémoire vive par fenêtres glissantes et n'est ni écrit sur disque ni transmis.
- **Permission minimale** : `RECORD_AUDIO` demandée uniquement au clic sur le bouton micro (« Dicter une mélodie »), jamais au lancement.

---

## 2. Pipeline audio -> portée

```
Micro (16 kHz mono PCM float)
  ├─> [1] Détecteur de hauteur f0 (CREPE-tiny / SPICE TFLite ~1–2 Mo, ou pYIN DSP)
  ├─> [2] Détecteur d'attaque / silence (RMS + flux spectral / saut de f0)
  ├─> [3] Lissage médian & quantification MIDI (note la plus proche, hystérésis ±35 cents)
  └─> [4] Quantification rythmique sur le tempo actif (noire, croche, blanche, silence)
        └─> Conversion éventuelle selon l'instrument transpositeur (Si♭, Mi♭, Fa)
              └─> Ajout en direct dans la partition courante
```

### 2.1 Détection de hauteur (`f0`)
- **Approche recommandée** : hybride léger ou modèle TFLite embarqué (`CREPE-tiny` quantifié ou `SPICE` TFLite, ~1 à 2 Mo) exécuté sur fenêtres de 1024 échantillons à 16 kHz (pas de 20 ms), avec score de confiance `voicing_prob ∈ [0, 1]`.
- **Repli DSP pur** : algorithme `pYIN` / autocorrélation normalisée en Kotlin/C++ pour comparer la latence et la précision sur les instruments à vent vs voix chantée.
- **Plage cible** : `C2` (~65 Hz) à `C7` (~2093 Hz), couvrant la voix humaine, le sifflet et les instruments d'harmonie/orchestre courants.

### 2.2 Découpage en notes et quantification rythmique
- **Segmentation** : une nouvelle note démarre lorsqu'une attaque est détectée (variation brusque d'énergie ou changement de demi-ton stable sur ≥ 3 trames consécutives, soit ~60 ms). Les micro-oscillations (vibrato < ±40 cents) restent fusionnées dans la note en cours.
- **Métronome de guidage** : clic optionnel (son ou retour haptique via `pilmil-core` pour ne pas repasser dans le micro) calé sur le BPM de la partition, avec grille configurable (à la noire, à la croche ou au triolet).
- **Correction immédiate** : les notes apparaissent en direct sur la portée ; l'utilisateur peut annuler la dernière mesure, ajuster une hauteur au doigt ou réécouter en MIDI.

---

## 3. Intégration avec la transposition Fredonne

C'est l'apport spécifique par rapport à un accordeur générique :
- Si la partition est configurée pour un instrument transpositeur (ex. **clarinette en Si♭**, **saxophone alto en Mi♭**, **cor en Fa**), l'utilisateur joue sa partie telle qu'il la lit.
- Le moteur applique automatiquement la transposition inverse entre la hauteur acoustique mesurée (`concert pitch`) et la note écrite sur la portée (`written pitch`), ou inversement selon le mode choisi (« Écrire en sons réels » / « Écrire pour mon instrument »).

---

## 4. Plan de réalisation par étapes

1. **Étape 1 — Prototype DSP + confiance (`pYIN` en `:shared` ou module audio)** : capture micro 16 kHz, affichage temps réel de la note détectée et du centrage en cents (utile aussi comme mini-accordeur intégré).
2. **Étape 2 — Comparatif `pYIN` vs modèle TFLite (`CREPE-tiny` / `SPICE`)** : benchmark sur jeu de test interne (voix chantée, sifflet, flûte, clarinette, guitare note à note) pour mesurer précision de hauteur, latence CPU et comportement en bruit ambiant.
3. **Étape 3 — Quantificateur rythmique + métronome haptique** : écriture directe dans le modèle de partition de Fredonne, gestion des silences et export MusicXML/MIDI inchangé.
