# Diagnostic du site (novembre 2026)

Mesures faites avec Chrome headless, pages `docs/index.html` et `docs/confidentialite.html`, en 1280 px et 360 px de large (iframe de largeur exacte), thèmes sombre et clair.

Constats avant reprise :
1. Le titre `h1` de l'accueil fait 60,8 px de corps dans une colonne de 568 px à 1280 px : 5 lignes. À 360 px, 35,2 px de corps : 5 lignes aussi.
2. Les 3 `<img>` de la section « Aperçu de l'application » n'ont ni `width` ni `height` (fichiers réels : 461 x 1000 px), avec `loading="lazy"` dans un conteneur à défilement horizontal. Affichées à 220 x 475 px, sans déformation ; la place n'est pas réservée avant chargement et la troisième est rognée à 360 px.
3. En-tête : 6 liens sur une ligne à 1280 px ; à 360 px, ils passent sur 3 lignes (67 px de haut), sans débordement. Chaque lien fait 24 px de haut.
4. Simulateur à 360 px : le cadre mesure 360 px mais son contenu atteint 372 px de large (rogné par `overflow:hidden`), et la barre d'outils coupe « Vider » et « Silence ». Dimensions inline (`style`) fixes : boutons de 30 à 33 px de haut, corps de 11 à 12 px ; 25 commandes sous 44 px de haut ou de large.
5. Contraste, thème sombre : texte courant 16,9:1, texte atténué 8,0:1, étiquette orange 8,7:1, texte du bouton principal 5,8:1 (4,6:1 à l'extrémité sombre de son dégradé).
6. Contraste, thème clair : étiquette en capitales (14,4 px) 4,7:1 sur le fond ; texte du bouton principal (blanc sur #B45309) 5,0:1 ; liens orange des cartes (« Découvrir … ») 4,3:1 sur le fond de carte, sous 4,5:1.
7. Cartes de fonctions : texte de 15,4 px dans une colonne de 199 px à 1280 px, soit environ 26 caractères par ligne, interligne 1,5.
8. Cartes « Fondements » : colonne de 285 px à 1280 px (environ 37 caractères par ligne) ; la 5e carte reste seule sur sa rangée. Le nombre de colonnes est fixé par un `style` en ligne.
9. Pied de page : liens de 20 px de haut à 1280 px, sans espacement vertical entre eux ; pas de lien d'évitement, pas de règle `@media print`.
10. Valeurs libres : 7 rayons différents (6, 12, 14, 20, 22, 999 px et un rayon à deux valeurs), 14 tailles de police différentes (0,68 à 1,3 rem, 18 px et 2 `clamp`), 1 ombre dans la feuille et 1 autre en `style` en ligne ; `confidentialite.html` a son propre `<style>` avec 5 couleurs, sans échelle commune.

## Après reprise

1. Corrigé : `h1` à 46,4 px, 3 lignes à 1280 px et 2 lignes à 800 px ; taille fluide (`clamp`), 4 lignes à 360 px (32,6 px), `text-wrap: balance`.
2. Corrigé : `width="461" height="1000"` ajoutés aux 3 images, rendu 220 x 475 px, ratio conservé. La cause d'un rendu sans captures n'a pas été reproduite en headless (les trois images s'affichent) ; `loading="lazy"` est conservé.
3. Corrigé en partie : aucun débordement à 360 px (largeur de page mesurée : 360), liens à 44 px de haut. Les 6 liens passent sur 3 lignes ; l'en-tête n'a pas été réduit à une ligne.
4. Corrigé : le cadre du simulateur ne rogne plus ses commandes à 360 px, commandes de 44 px minimum, polices de 12 px minimum (surcharge CSS des tailles en ligne). Touches blanches du clavier à 56 px sous 700 px (défilement horizontal du clavier dans son cadre). Non corrigé : touches noires de 27 x 62 px au-dessus de 700 px de large (44 px en dessous).
5. Aucune correction nécessaire en thème sombre (5,8:1 ; 4,6:1 en bout de dégradé).
6. Corrigé : orange du thème clair #B45309 remplacé par #A04B08 pour le texte (5,1:1 sur fond de carte, 5,7:1 sur le fond) ; l'étiquette passe de 4,7:1 à 5,7:1. Le fond du bouton principal reste #B45309 (blanc dessus : 5,0:1).
7. Corrigé : cartes sur deux colonnes à partir de 700 px, environ 55 caractères par ligne, interligne 1,6.
8. Corrigé : même règle pour les cartes « Fondements » (style en ligne retiré) ; la 5e carte reste seule sur sa rangée.
9. Corrigé : liens du pied de page à 44 px de haut, lien d'évitement ajouté sur l'accueil, règle `@media print` sur les deux pages, `prefers-reduced-motion`. Non fait : lien d'évitement sur `confidentialite.html` (page d'une colonne sans navigation).
10. Corrigé : variables pour couleurs, 7 tailles de police, 6 espacements, 4 rayons, 2 ombres. Restent en ligne dans le HTML : couleurs et dimensions de la maquette du simulateur.
