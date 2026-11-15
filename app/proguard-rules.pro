# Fredonne n'utilise pas de réflexion applicative : les règles par défaut d'Android suffisent.
# Les activités et le FileProvider sont conservés via le manifeste.
-keep class androidx.core.content.FileProvider { *; }
