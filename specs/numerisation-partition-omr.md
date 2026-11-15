# Spécification technique — Numérisation de partition (OMR monophonique hors-ligne)

## 1. Objectif
Permettre à l'utilisateur de photographier (ou importer en image) une partition papier simple à une voix afin de la convertir localement en séquence `Melody`, puis de l'écouter, de la transposer (Si♭, Mi♭, Fa…) et de l'exporter en MusicXML ou MIDI sans connexion réseau.

## 2. Périmètre v1 (strict)
- **Inclus** :
  - Partitions **imprimées** (gravure standard),
  - **Une seule voix** sur **une seule portée** par système (partie d'instrument à vent, ligne de chant, thème mélodique),
  - Clé de Sol et clé de Fa, armure (`♯` / `♭`), chiffrage de mesure simple (`2/4`, `3/4`, `4/4`, `6/8`),
  - Figures de base : ronde, blanche, noire, croche, double croche, pointées, silences correspondants, altérations accidentelles, barres de mesure.
- **Exclus en v1** :
  - Piano / orgue (systèmes à 2+ portées reliées par accolade),
  - Polyphonie sur une même portée (accords, deux voix avec hampes opposées),
  - Partitions manuscrites, tablatures, paroles / nuances complexes.

## 3. Pipeline technique (100 % sur appareil)

### Étape 1 — Prétraitement & redressement
- Conversion niveaux de gris + correction de perspective / inclinaison (*deskew*) via les bords de page ou l'angle dominant des lignes de portée.
- Binarisation adaptative locale (type Sauvola) pour compenser les ombres et le papier jauni.

### Étape 2 — Segmentation des portées & échelle (`staff_space`)
- Détection des 5 lignes de chaque portée par projection horizontale / suivi de composantes.
- Calcul de `staff_space` (distance moyenne entre deux lignes consécutives) : toute la géométrie verticale (hauteur de note) et horizontale (découpage en mesures) est normalisée par rapport à `staff_space`, ce qui rend le système indépendant de la résolution photo.
- Découpage de la page en bandes horizontales (un système = une portée à la fois).

### Étape 3 — Détection des symboles (modèle embarqué)
- Détecteur d'objets léger (ex. YOLO11n exporté en **TFLite / ONNX** < 10 Mo) appliqué par bande de portée.
- Classes minimales :
  - `clef_g`, `clef_f`
  - `sharp`, `flat`, `natural`
  - `time_sig_digit`
  - `notehead_filled`, `notehead_hollow`, `whole_note`
  - `flag_8th`, `flag_16th`, `beam`, `dot`
  - `rest_whole_half`, `rest_quarter`, `rest_8th`, `rest_16th`
  - `barline`
- **Entraînement (Kaggle)** : prévoir un audit de licence strict avant tout entraînement (ex. *PrIMuS / Camera-PrIMuS*, *DeepScores*, *DoReMi* — n'utiliser que des jeux compatibles avec une distribution libre MPL-2.0).

### Étape 4 — Reconstruction logique (`Melody`)
- **Hauteur** : position verticale du centre de `notehead_*` par rapport aux 5 lignes de la portée (`y` quantifié par pas de `0.5 × staff_space`), combinée à la clé, l'armure et les altérations accidentelles de la mesure.
- **Durée** : combinaison tête (`hollow`/`filled`/`whole`) + présence de crochets/ligatures (`flag`/`beam`) + point (`dot`).
- **Validation rythmique** : contrôle de la somme des durées par mesure par rapport au chiffrage détecté ; les mesures incohérentes sont signalées visuellement à l'utilisateur pour correction rapide.

## 4. UX & garde-fous
- **Guidage à la prise de vue** : cadre horizontal à l'écran + alerte si l'image est trop floue ou trop inclinée avant de lancer l'analyse.
- **Écran de révision obligatoire avant export** : affichage immédiat de la portée reconstruite avec surlignage des mesures dont le compte rythmique ne tombe pas juste, et lecture audio directe pour repérer à l'oreille une note fausse.
- **Aucun envoi serveur** : image et traitement restent en mémoire locale (suppression du fichier temporaire après reconstruction).

## 5. Pourquoi le polyphonique et le multi-portées sont exclus (limites assumées)

Le choix de borner la v1 à **une seule portée imprimée à une voix** est volontaire. Étendre l'OMR au polyphonique ou au multi-portées (piano, guitare, conducteur) change la nature du problème sur quatre plans :

1. **Collisions et empilements visuels** : dans un accord, les notes à intervalle de seconde sont décalées de part et d'autre de la hampe et les altérations (`♯`, `♭`, `♮`) s'empilent en se chevauchant, ce qui complique leur association exacte à chaque tête de note.
2. **Séparation des voix sur une même portée** : deux voix simultanées (hampes opposées, silences propres à chaque voix, rythmes décalés) empêchent une lecture linéaire gauche→droite ; il faut reconstruire un graphe syntaxique complet et valider le rythme voix par voix.
3. **Alignement vertical inter-portées** : sur un système à plusieurs portées, les temps doivent être synchronisés verticalement malgré les décalages de gravure ; la moindre erreur d'alignement désynchronise toute la suite de la lecture et de l'export MusicXML.
4. **Coût modèle et modèle de données** : une mélodie à une voix produit une séquence linéaire compatible avec `Melody` et un détecteur léger sur téléphone d'entrée de gamme. Le polyphonique exige des architectures *end-to-end* nettement plus lourdes en mémoire/CPU et remettrait en cause le périmètre « une voix » de Fredonne.

