package systems.diath.visotaris.services

import systems.diath.visotaris.VisotarisLogger
import systems.diath.visotaris.config.ConfigManager

/**
 * Expandiert Mengenkurzformen in OPSUCHT-Commands vor dem Absenden.
 *
 * Beispiele:
 *   /pay DiamantTh 1.5k   →   /pay DiamantTh 1500
 *   bank einzahlen 2m      →   bank einzahlen 2000000
 *   bank auszahlen 500k    →   bank auszahlen 500000
 *
 * Unterstützte Suffixe (Groß-/Kleinschreibung egal):
 *   k = 1.000    m = 1.000.000    b = 1.000.000.000
 *
 * Aktivierbar über VisotarisConfig.enableCommandShortforms (Standard: true).
 */
class CommandRewriteService(private val config: ConfigManager) {

    /**
     * Gibt den (ggf. umgeschriebenen) Command zurück.
     * Wird mit MODIFY_COMMAND vor ALLOW_COMMAND aufgerufen.
     */
    fun rewrite(rawCommand: String): String {
        if (!config.getConfig().enableCommandShortforms) return rawCommand

        // Nur bekannte Geld-Commands umschreiben
        val lower = rawCommand.trimStart('/').lowercase()
        val isMoneyCommand = MONEY_PREFIXES.any { lower.startsWith(it) }
        if (!isMoneyCommand) return rawCommand

        val rewritten = SHORTHAND_RE.replace(rawCommand) { match ->
            expandAmount(match.groupValues[1], match.groupValues[2]) ?: match.value
        }

        if (rewritten != rawCommand) {
            VisotarisLogger.debug("CommandRewrite: '{}' → '{}'", rawCommand, rewritten)
        }
        return rewritten
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun expandAmount(digits: String, suffix: String): String? {
        // Deutschen Tausenderpunkt entfernen, dann ggf. Komma zu Punkt
        val normalized = digits.replace(".", "").replace(",", ".")
        val number = normalized.toDoubleOrNull() ?: return null
        val multiplier = when (suffix.lowercase()) {
            "k" -> 1_000.0
            "m" -> 1_000_000.0
            "b" -> 1_000_000_000.0
            else -> return null
        }
        return (number * multiplier).toLong().toString()
    }

    companion object {
        /** Commands, bei denen Kurzformen ausgewertet werden. */
        private val MONEY_PREFIXES = listOf(
            "pay ",
            "bank einzahlen",
            "bank auszahlen",
            "sell ",
            "auction ",
            "bid ",
        )

        /**
         * Passt auf: 1k  1.5k  1,5k  2m  500k  1b  usw.
         * Kein Leerzeichen zwischen Zahl und Suffix.
         */
        private val SHORTHAND_RE = Regex(
            """(\d+(?:[.,]\d+)?)([kmb])""",
            RegexOption.IGNORE_CASE,
        )
    }
}
