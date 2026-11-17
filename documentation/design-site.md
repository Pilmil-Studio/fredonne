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
10. Valeurs libres : environ 20 rayons, 6 tailles de police et 4 ombres différents dans le `<style>` ; `confidentialite.html` a ses propres couleurs, sans échelle commune.
