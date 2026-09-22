package com.moeketsifaso.ntsamaise

import java.util.Locale

internal enum class CurrencyReadingStatus {
    POSSIBLE_MATCH, NO_TEXT, MISSING_IDENTITY, CONFLICTING_TEXT, INSUFFICIENT_EVIDENCE
}

internal data class CurrencyReading(
    val status: CurrencyReadingStatus,
    val denomination: Int? = null
) {
    val message: String
        get() = when (status) {
            CurrencyReadingStatus.POSSIBLE_MATCH ->
                "Possible $denomination maloti, based on printed text. Check the note before using this result."
            CurrencyReadingStatus.NO_TEXT ->
                "No text found. Hold one note flat in good light and scan again."
            CurrencyReadingStatus.MISSING_IDENTITY ->
                "Could not identify Lesotho banknote text. Show the side with the bank name and scan again."
            CurrencyReadingStatus.CONFLICTING_TEXT ->
                "Conflicting currency or denomination text. Show only one note and scan again."
            CurrencyReadingStatus.INSUFFICIENT_EVIDENCE ->
                "Could not read the denomination clearly. Show the whole note, move closer and scan again."
        }
}

/**
 * A conservative text heuristic, not a banknote classifier or an authenticity check.
 * Supported values follow https://centralbank.org.ls/currency/.
 * Do not repair O/0 or I/1: guessing a digit can change the monetary value.
 */
internal object LotiCurrencyReader {
    private val supported = setOf(10, 20, 50, 100, 200)
    private val issuer = Regex("\\b(?:CENTRAL BANK OF LESOTHO|BANKA E KHOLO EA LESOTHO)\\b")
    private val currency = Regex("\\bMALOTI\\b")
    private val foreignCurrency = Regex(
        "\\b(?:RAND|RANDS|SOUTH AFRICAN RESERVE BANK|EMALANGENI|LILANGENI|DOLLARS?|EUROS?)\\b"
    )
    // Ignore embedded serial digits, signed values, dates, decimals and grouped numbers.
    private val numeral = Regex("(?<![\\p{L}\\p{N}.,/+\\-])(?:200|100|50|20|10)(?![\\p{L}\\p{N}.,/])")
    private val words = Regex("\\b(TWO HUNDRED|ONE HUNDRED|FIFTY|TWENTY|TEN) MALOTI\\b")
    private val wordValues = mapOf(
        "TEN" to 10, "TWENTY" to 20, "FIFTY" to 50,
        "ONE HUNDRED" to 100, "TWO HUNDRED" to 200
    )
    private val labelledAmount = Regex("(?<![\\p{L}\\p{N}.,/+\\-])([0-9]+) MALOTI\\b")

    fun read(rawText: String): CurrencyReading {
        val text = rawText.uppercase(Locale.ROOT).replace(Regex("[\\s\\u00A0]+"), " ").trim()
        if (text.isEmpty()) return CurrencyReading(CurrencyReadingStatus.NO_TEXT)
        if (!issuer.containsMatchIn(text) || !currency.containsMatchIn(text)) {
            return CurrencyReading(CurrencyReadingStatus.MISSING_IDENTITY)
        }
        if (foreignCurrency.containsMatchIn(text)) {
            return CurrencyReading(CurrencyReadingStatus.CONFLICTING_TEXT)
        }

        val numericValues = numeral.findAll(text).map { it.value.toInt() }.toList()
        val spelledValues = words.findAll(text).map { wordValues.getValue(it.groupValues[1]) }.toList()
        val candidates = (numericValues + spelledValues).toSet()
        val unsupportedAmount = labelledAmount.findAll(text).any {
            it.groupValues[1].toIntOrNull() !in supported
        }
        if (candidates.size > 1 || unsupportedAmount) {
            return CurrencyReading(CurrencyReadingStatus.CONFLICTING_TEXT)
        }
        val value = candidates.singleOrNull()
        // Two printed numeral occurrences, or a numeral corroborated by an English amount.
        // A single number, only words, or currency/issuer text alone is insufficient.
        val count = numericValues.count { it == value }
        if (value == null || count == 0 || (count < 2 && value !in spelledValues)) {
            return CurrencyReading(CurrencyReadingStatus.INSUFFICIENT_EVIDENCE)
        }
        return CurrencyReading(CurrencyReadingStatus.POSSIBLE_MATCH, value)
    }
}
