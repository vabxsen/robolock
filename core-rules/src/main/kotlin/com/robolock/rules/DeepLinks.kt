package com.robolock.rules

/**
 * A URL shape that identifies a specific surface.
 *
 * These are kept narrow on purpose. A pattern that matched all of instagram.com would make
 * Robolock a plausible handler for every Instagram link on the device, which would hijack links
 * the user expects to open in Instagram itself. Only paths that unambiguously name a short-form
 * destination belong here.
 */
data class DeepLinkPattern(
    val surface: DeepLinkSurface,
    val hosts: Set<String>,
    val pathRegex: Regex,
)

data class DeepLinkMatch(val surface: DeepLinkSurface, val uri: String)

object DeepLinkRuleEngine {

    val patterns: List<DeepLinkPattern> = listOf(
        DeepLinkPattern(
            surface = YouTubeShorts,
            hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"),
            pathRegex = Regex("^/shorts/[\\w-]+/?$"),
        ),
        DeepLinkPattern(
            surface = InstagramReels,
            hosts = setOf("instagram.com", "www.instagram.com"),
            // /reel/ and /reels/ only. /p/ is a normal post and must not match.
            pathRegex = Regex("^/reels?/[\\w-]+/?$"),
        ),
    )

    /**
     * Identifies a short-form destination from a URL, or null.
     *
     * Returning null is the common and correct outcome — it means Robolock has no honest basis
     * for claiming to know what the link opens, so it gets out of the way.
     */
    fun match(rawUri: String): DeepLinkMatch? {
        val parsed = parse(rawUri) ?: return null
        val (host, path) = parsed
        val pattern = patterns.firstOrNull { p ->
            host in p.hosts && p.pathRegex.matches(path)
        } ?: return null
        return DeepLinkMatch(pattern.surface, rawUri)
    }

    /** Minimal, dependency-free URL splitting: enough for host and path, nothing more. */
    private fun parse(raw: String): Pair<String, String>? {
        val withoutScheme = raw.substringAfter("://", missingDelimiterValue = "")
            .ifEmpty { return null }
        val authority = withoutScheme.substringBefore('/')
        val host = authority.substringAfter('@').substringBefore(':').lowercase()
        if (host.isEmpty()) return null
        val path = "/" + withoutScheme.substringAfter('/', missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
        return host to path
    }
}
