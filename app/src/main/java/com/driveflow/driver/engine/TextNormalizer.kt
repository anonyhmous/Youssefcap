package com.driveflow.driver.engine

import java.util.Locale

/**
 * Pure Kotlin text normalization for Arabic and Latin text used by [DecisionEngine].
 *
 * Rules:
 * - Remove diacritics (Arabic Tashkeel: Fatha, Damma, Kasra, Sukun, Shadda, Tanween, Superscript Alif)
 * - Remove Tatweel (Kashida '\u0640')
 * - Unify variants of Alif: 'أ', 'إ', 'آ', 'ٱ' -> 'ا'
 * - Unify Alif Maqsura: 'ى' -> 'ي'
 * - Unify Ta Marbuta: 'ة' -> 'ه'
 * - Lowercase Latin letters
 * - Collapse consecutive whitespace and trim
 */
object TextNormalizer {

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder(input.length)

        for (ch in input) {
            when {
                // Diacritics (Tashkeel) and zero-width/bidi markers: skip
                ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' ||
                ch == '\u200E' || ch == '\u200F' || ch in '\u202A'..'\u202E' ||
                ch in '\u2066'..'\u2069' || ch in '\u200B'..'\u200D' || ch == '\uFEFF' -> {
                    // Ignored
                }
                // Unify Alifs
                ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ' -> sb.append('ا')
                // Unify Alif Maqsura to Ya
                ch == 'ى' -> sb.append('ي')
                // Unify Ta Marbuta to Ha
                ch == 'ة' -> sb.append('ه')
                // Arabic-Indic to ASCII digits
                ch in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                ch in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                // Latin lowercase
                ch in 'A'..'Z' -> sb.append(ch.lowercaseChar())
                // Collapse whitespace / non-breaking space
                ch.isWhitespace() || ch == '\u00A0' -> sb.append(' ')
                else -> sb.append(ch)
            }
        }

        // Collapse multiple spaces into one and trim
        return sb.toString()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Area matching: normalized contains in either direction with a minimum length of 3.
     */
    fun matchesArea(address: String?, area: String?): Boolean {
        if (address.isNullOrBlank() || area.isNullOrBlank()) return false
        val normAddress = normalize(address)
        val normArea = normalize(area)
        if (normAddress.length < 3 || normArea.length < 3) return false
        return normAddress.contains(normArea) || normArea.contains(normAddress)
    }

    /**
     * Checks if the given address matches any of the areas in the list.
     */
    fun matchesAnyArea(address: String?, areas: List<String>): Boolean {
        if (address.isNullOrBlank() || areas.isEmpty()) return false
        return areas.any { area -> matchesArea(address, area) }
    }

    /**
     * Checks if the note contains any of the banned words (normalized comparison).
     */
    fun containsBannedWord(note: String?, bannedWords: List<String>): Boolean {
        if (note.isNullOrBlank() || bannedWords.isEmpty()) return false
        val normNote = normalize(note)
        return bannedWords.any { banned ->
            val normBanned = normalize(banned)
            if (normBanned.length >= 2) {
                normNote.contains(normBanned)
            } else {
                false
            }
        }
    }
}
