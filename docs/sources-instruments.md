# Sources des transpositions et des tessitures

Vérification faite le 16 novembre 2026, sur les pages listées en fin de document. Le code de référence est `core/music/src/main/kotlin/fr/fredonne/core/music/Instruments.kt`, qui n'a pas été modifié.

## Méthode et convention

- La tessiture du code est donnée en hauteurs écrites (`writtenLow`, `writtenHigh`). Pour les instruments non transpositeurs, la hauteur écrite est aussi la hauteur sonore.
- Pour chaque transposition, la hauteur sonore donnée par la source pour un do écrit (do 4) a été comparée à celle du code, en demi-tons. Les écarts de désignation d'intervalle dans les sources ont été recalculés à partir des noms de notes, et non repris tels quels.
- Verdicts : « concorde » si la source donne la valeur ou une valeur qui en découle ; « écart » si la source donne une autre valeur ; « non vérifié » si aucune source ouverte ne le dit.

## Points critiques

**Transpositions : aucun écart trouvé.** Les dix instruments transpositeurs du code (clarinette, saxophones, trompette, cor en fa, guitare, guitare basse, contrebasse) concordent avec les sources. Trois points restent à trancher par un humain :

1. **Trombone.** Le code le traite comme non transpositeur, ce qui correspond au trombone ténor en ut (orchestre). Un trombone en si bémol, écrit en clé de sol, est signalé dans une table de référence comme sonnant une neuvième plus bas. Cette variante n'est pas modélisée et la source est unique. Si Fredonne vise les harmonies, où le trombone en si bémol est courant, la question est à trancher.
2. **Harpe à pédales, extrémité aiguë.** Le code va jusqu'à sol dièse 7. Les sources divergent : la page Wikipédia indique sol bémol 7, la table orchestrale indique sol dièse 7 (la bémol 7). Un calcul sur 47 cordes en do bémol majeur (do bémol 1 à sol bémol 7) donne sol bémol 7. Ce point touche la tessiture, pas la transposition.
3. **Contrebasse et guitare, extrémités hautes.** Le code est plus restrictif que les sources sur le haut de tessiture (détail ci-dessous).

## Transpositions

| Instrument (`id` du code) | Transposition du code | Verdict | Sources |
|---|---|---|---|
| Flûte (`flute`) | aucune | concorde (la table indique une flûte en ut, sans transposition) | S11 |
| Hautbois (`oboe`) | aucune | non vérifié (aucune source ouverte ne parle du hautbois) | S11 ne le mentionne pas |
| Clarinette si bémol (`clarinet_bb`) | une seconde majeure plus bas | concorde | S1, S3, S11 |
| Saxophone soprano (`sax_soprano`) | une seconde majeure plus bas | concorde | S1 |
| Saxophone alto (`sax_alto`) | une sixte majeure plus bas | concorde | S1, S9, S11 |
| Saxophone ténor (`sax_tenor`) | une neuvième majeure plus bas | concorde | S1, S11 |
| Saxophone baryton (`sax_baritone`) | une treizième majeure plus bas (octave et sixte) | concorde (calcul depuis la liste S1 : do 4 écrit donne mi bémol 2) | S1 |
| Trompette si bémol (`trumpet_bb`) | une seconde majeure plus bas | concorde | S1, S2, S11 |
| Cor en fa (`horn_f`) | une quinte juste plus bas | concorde | S1, S2, S10, S11 |
| Trombone (`trombone`) | aucune (ténor en ut) | concorde pour le trombone ténor ; variante en si bémol non modélisée (voir point 1) | S11, S12 |
| Violon (`violin`) | aucune, sauf scordatura | concorde | S11 |
| Harpe celtique (`harp_celtic`) | aucune | non vérifié | aucune |
| Harpe à pédales (`harp_pedal`) | aucune | concorde (aucune transposition signalée) | S5, S11 |
| Guitare (`guitar`) | une octave plus bas | concorde | S2, S1, S11 |
| Guitare basse électrique (`bass_electric`) | une octave plus bas | concorde : la notation est une octave au-dessus du son, en clé de fa | S2, S8 |
| Contrebasse (`contrabass`) | une octave plus bas | concorde | S2, S4 |
| Voix (`voice_soprano`, `voice_alto`, `voice_tenor`, `voice_bass`) | aucune | sans objet (hauteurs chantées) | |

## Tessitures (hauteurs écrites)

| Instrument | Tessiture du code | Source et valeur | Verdict |
|---|---|---|---|
| Flûte | do 4 à do 7 | S11 : do 4 à ré 7 | écart (aigu : do 7 au lieu de ré 7) |
| Hautbois | si bémol 3 à la 6 | S11 : si bémol 3 à la 6 | concorde |
| Clarinette si bémol | mi 3 à do 7 | S3 et S11 : mi 3 à do 7 | concorde |
| Saxophone soprano | si bémol 3 à fa 6 | S11 (ligne commune aux saxophones) : si bémol 3 à sol 6 | écart mineur (aigu) |
| Saxophone alto | si bémol 3 à fa 6 | S9 : si bémol 3 à fa 6 (ou fa dièse 6) ; S11 : si bémol 3 à sol 6 | concorde avec S9 ; écart mineur avec S11 |
| Saxophone ténor | si bémol 3 à fa 6 | S11 : si bémol 3 à sol 6 | écart mineur (aigu) |
| Saxophone baryton | si bémol 3 à fa 6 | non vérifié spécifiquement | non vérifié |
| Trompette si bémol | fa dièse 3 à do 6 | S11 : fa dièse 3 à ré 6 | écart mineur (aigu) |
| Cor en fa | fa dièse 2 à do 6 | S10 (sonore : si 1 à fa 5, donc écrit fa dièse 2 à do 6) ; S11 : fa dièse 2 à do 6 | concorde |
| Trombone | mi 2 à si bémol 4 | S12 et S11 (trombone ténor) : mi 2 à fa 5 | écart (aigu : si bémol 4 au lieu de fa 5) |
| Violon | sol 3 à mi 7 | S7 : sol 3 et mi 7 (aigu « peu défini ») ; S11 : sol 3 à la 7 | concorde avec S7 ; écart avec S11 |
| Harpe celtique | do 2 à la 6 | non vérifié | non vérifié |
| Harpe à pédales | do bémol 1 à sol dièse 7 | S5 : do bémol 1 à sol bémol 7 ; S11 : do bémol 1 à sol dièse 7 (la bémol 7) | écart (aigu, sources divergentes ; voir point 2) |
| Guitare | mi 3 à si 6 | S11 : mi 3 à mi 6 | écart (aigu : si 6 au lieu de mi 6) |
| Guitare basse électrique | mi 2 à sol 4 | S8 : mi 1 sonore (donc mi 2 écrit) ; aigu non vérifié | partiel (grave concorde, aigu non vérifié) |
| Contrebasse | mi 2 à ré 4 | S11 : do 2 à do 5 écrits ; S4 : mi 1 sonore, aigu proche de ré 5 sonore | écart (aigu : ré 4 écrit, soit ré 3 sonore, plus bas que les deux sources) ; grave concorde avec S4 |
| Voix soprano | do 4 à do 6 | S6 : do 4 à do 6 | concorde |
| Voix alto | sol 3 à ré 5 | S13 : fa 3 à ré 5 (Harvard) ; sol 3 à mi 5 (Grove) | écart mineur (grave) ; aigu concorde avec Harvard et est inclus dans Grove |
| Voix ténor | do 3 à do 5 | S6 : si 2 à la 4 ; S13 : si 2 à sol 4 (Harvard), do 3 à la 4 (Grove) | écart (aigu : do 5 au-delà des sources ; la tessiture du code correspond plutôt à un usage choral) |
| Voix basse | mi 2 à mi 4 | S6 : mi 2 à mi 4 ; S13 : mi 2 à do 4 (Harvard), fa 2 à mi 4 (Grove) | concorde avec S6 |

## Détail des sources ouvertes

- **S1.** Wikipédia (anglais), « List of transposing instruments ». Wikimedia Foundation, page consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/List_of_transposing_instruments
- **S2.** Wikipédia (anglais), « Transposing instrument ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Transposing_instrument
- **S3.** Wikipédia (anglais), « Clarinet ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Clarinet
- **S4.** Wikipédia (anglais), « Double bass ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Double_bass
- **S5.** Wikipédia (anglais), « Pedal harp ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Pedal_harp
- **S6.** Wikipédia (anglais), « Vocal range », copie sur un nœud IPFS. Consultée le 16 novembre 2026. https://ipfs.ssi.eecc.de/ipns/en.wikipedia-on-ipfs.org/wiki/Vocal_range
- **S7.** Wikipédia (anglais), « Violin ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Violin
- **S8.** Wikipédia (anglais), « Bass guitar tuning ». Wikimedia Foundation, consultée le 16 novembre 2026. https://en.wikipedia.org/wiki/Bass_guitar_tuning
- **S9.** Wikipédia (anglais), « Alto sax », copie Kiwix de l'édition d'octobre 2023. https://kaweah.freedombox.rocks/kiwix/wikipedia_en_all_maxi_2023-10/Alto_sax
- **S10.** New World Encyclopedia, « French horn ». Consultée le 16 novembre 2026. https://www.newworldencyclopedia.org/entry/French_horn
- **S11.** Orchestral Library, « Range reference table » (tableau des tessitures et transpositions, colonne « written range »). Consultée le 16 novembre 2026. https://orchestralibrary.com/reftables/rang.html
- **S12.** Vienna Symphonic Library (VSL), « Tenor trombone : Range ». Consultée le 16 novembre 2026. https://vsl.co.at/en/Tenor_trombone/Range
- **S13.** Bibliothèque de l'Université Yale, « Vocal ranges », reprenant le New Harvard Dictionary of Music et Grove Music Online. Consultée le 16 novembre 2026. https://web.library.yale.edu/printpdf/cataloging/music/vocal-ranges

Les pages S1, S2 et S3 ont été lues par un outil d'extraction qui a parfois donné des intervalles inexacts ; les verdicts ci-dessus reposent sur les noms de notes, recalculés à la main.

## Non vérifié

- Hautbois : aucune source ouverte sur la transposition ni la tessiture (S11 donne la tessiture seulement).
- Saxophone baryton : tessiture non vérifiée, transposition vérifiée seulement par S1.
- Harpe celtique : tessiture et absence de transposition non vérifiées.
- Guitare basse électrique : aigu (sol 4) non vérifié.
- Voix : pas de source sur la clé 8vb du ténor ni sur l'affichage de la hauteur sonore dans cette clé.
