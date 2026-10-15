package systems.diath.visotaris_opmod.web

/**
 * Read-only selection of pre-compressed WebUI resources. The logical request
 * path remains unchanged; only the classpath resource path gains a suffix.
 */
enum class StaticResourceEncoding(val token: String?, val suffix: String) {
    BROTLI("br", ".br"),
    ZSTANDARD("zstd", ".zst"),
    RAW(null, "")
}

data class StaticResourceVariant(
    val requestedPath: String,
    val resourcePath: String,
    val encoding: StaticResourceEncoding
)

object StaticResourceCompression {
    private val compressibleExtensions = setOf(".js", ".css", ".html", ".json", ".svg")

    fun isCompressible(resourcePath: String): Boolean =
        compressibleExtensions.any { resourcePath.endsWith(it, ignoreCase = true) }

    fun requiresVary(resourcePath: String): Boolean = isCompressible(resourcePath)

    fun isSafeStaticPath(path: String): Boolean = path.isNotBlank() && path
        .split('/')
        .all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." &&
                segment.none { it == '\\' || it == '\u0000' }
        }

    fun select(
        resourcePath: String,
        acceptEncoding: String?,
        resourceExists: (String) -> Boolean
    ): StaticResourceVariant {
        if (!isCompressible(resourcePath)) {
            return StaticResourceVariant(resourcePath, resourcePath, StaticResourceEncoding.RAW)
        }

        val accepted = acceptedEncodings(acceptEncoding)
        for (encoding in listOf(StaticResourceEncoding.BROTLI, StaticResourceEncoding.ZSTANDARD)) {
            if (accepts(accepted, encoding) && resourceExists(resourcePath + encoding.suffix)) {
                return StaticResourceVariant(resourcePath, resourcePath + encoding.suffix, encoding)
            }
        }
        return StaticResourceVariant(resourcePath, resourcePath, StaticResourceEncoding.RAW)
    }

    private fun accepts(accepted: Map<String, Double>, encoding: StaticResourceEncoding): Boolean {
        val token = encoding.token ?: return true
        val quality = accepted[token] ?: accepted["*"] ?: 0.0
        return quality > 0.0
    }

    private fun acceptedEncodings(header: String?): Map<String, Double> {
        if (header.isNullOrBlank()) return emptyMap()
        return buildMap {
            header.split(',').forEach { entry ->
                val parameters = entry.trim().split(';')
                val token = parameters.firstOrNull()?.trim()?.lowercase().orEmpty()
                if (token.isBlank()) return@forEach
                val quality = parameters.drop(1)
                    .firstOrNull { it.trim().startsWith("q=", ignoreCase = true) }
                    ?.substringAfter('=')
                    ?.trim()
                    ?.toDoubleOrNull()
                    ?.takeIf { it in 0.0..1.0 }
                    ?: if (parameters.any { it.trim().startsWith("q=", ignoreCase = true) }) 0.0 else 1.0
                put(token, maxOf(get(token) ?: 0.0, quality))
            }
        }
    }
}
