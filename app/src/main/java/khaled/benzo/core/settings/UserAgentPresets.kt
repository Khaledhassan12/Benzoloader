package khaled.benzo.core.settings

/**
 * Choices for the default user-agent. The stored setting stays one plain string (so backups keep working);
 * a preset is just a known string, and anything else is "custom". The strings are generic, commonly used browser formats.
 */
object UserAgentPresets {
    const val CUSTOM = -1

    /** Index 0 is "Default": blank, meaning the HTTP client's own value. */
    val values: List<String> = listOf(
        "",
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
    )

    /** Preset index for a stored value (blank counts as Default), or [CUSTOM]. */
    fun indexOf(stored: String): Int {
        val t = stored.trim()
        val i = values.indexOf(t)
        return if (i >= 0) i else CUSTOM
    }
}
