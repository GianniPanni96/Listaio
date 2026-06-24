package com.listaio.util

import java.text.Normalizer

/**
 * Normalizes an item name for duplicate detection and matching:
 * trims, lowercases, collapses internal whitespace and strips accents — so "Latte",
 * "latte ", "  LATTE" and "latté" all compare as equal, while genuinely different names
 * (e.g. "mela" vs "mele") stay distinct.
 */
fun normalizeName(raw: String): String {
    val collapsed = raw.trim().lowercase().replace(WHITESPACE, " ")
    val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
    return decomposed.replace(COMBINING_MARKS, "")
}

private val WHITESPACE = Regex("\\s+")
private val COMBINING_MARKS = Regex("\\p{Mn}+")
