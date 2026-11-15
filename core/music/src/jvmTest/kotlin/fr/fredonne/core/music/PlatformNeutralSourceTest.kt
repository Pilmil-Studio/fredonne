package fr.fredonne.core.music

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformNeutralSourceTest {

    private val forbiddenTokens = listOf(
        "import java.",
        "Math.",
        "System.",
    )

    @Test
    fun commonMainDoesNotUseJvmOnlyApis() {
        val mainDir = listOf(
            File("src/main/kotlin"),
            File("core/music/src/main/kotlin"),
        ).firstOrNull { it.isDirectory }
            ?: error("Répertoire src/main/kotlin introuvable depuis ${File(".").absolutePath}")

        val ktFiles = mainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertTrue(ktFiles.isNotEmpty(), "Aucun fichier Kotlin trouvé dans ${mainDir.path}")

        val violations = mutableListOf<String>()
        for (file in ktFiles) {
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed
                for (token in forbiddenTokens) {
                    if (token in line) {
                        violations += "${file.relativeTo(mainDir).path}:${index + 1} -> $token"
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "API JVM détectée(s) dans core/music/src/main/kotlin :\n${violations.joinToString("\n")}",
        )
    }
}
