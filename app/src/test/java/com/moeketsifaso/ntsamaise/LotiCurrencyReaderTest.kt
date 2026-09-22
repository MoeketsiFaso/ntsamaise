package com.moeketsifaso.ntsamaise

import org.junit.Assert.*
import org.junit.Test

class LotiCurrencyReaderTest {
    private val issuer = "CENTRAL BANK OF LESOTHO"

    @Test
    fun allSupportedValuesNeedCorroboratingText() {
        listOf(10, 20, 50, 100, 200).forEach { value ->
            val result = LotiCurrencyReader.read("$issuer\n$value MALOTI\n$value")
            assertEquals(CurrencyReadingStatus.POSSIBLE_MATCH, result.status)
            assertEquals(value, result.denomination)
            assertTrue(result.message.startsWith("Possible $value maloti"))
        }
    }

    @Test
    fun supportsSesothoIssuerWithRepeatedNumerals() {
        val result = LotiCurrencyReader.read("Banka e kholo ea Lesotho\nMALOTI\n50\n50")
        assertEquals(50, result.denomination)
    }

    @Test
    fun anEnglishAmountCorroboratesASingleNumeral() {
        mapOf(10 to "TEN", 20 to "TWENTY", 50 to "FIFTY", 100 to "ONE HUNDRED", 200 to "TWO HUNDRED")
            .forEach { (value, words) ->
                assertEquals(value, LotiCurrencyReader.read("$issuer\n$words MALOTI\n$value").denomination)
            }
    }

    @Test
    fun handlesLineBreaksAndNonBreakingSpaces() {
        assertEquals(20, LotiCurrencyReader.read(
            "central\nbank\u00a0of lesotho\ntwenty\nmaloti\n20"
        ).denomination)
    }

    @Test
    fun blankCaptureIsNotAMatch() {
        assertEquals(CurrencyReadingStatus.NO_TEXT, LotiCurrencyReader.read(" \n ").status)
    }

    @Test
    fun numbersAndCurrencyNameWithoutIssuerAreNotAMatch() {
        listOf("20 20", "20 MALOTI 20", "LESOTHO 20 MALOTI 20").forEach {
            assertEquals(CurrencyReadingStatus.MISSING_IDENTITY, LotiCurrencyReader.read(it).status)
        }
    }

    @Test
    fun issuerWithoutCurrencyNameIsNotAMatch() {
        assertEquals(CurrencyReadingStatus.MISSING_IDENTITY, LotiCurrencyReader.read("$issuer 100 100").status)
    }

    @Test
    fun aSingleNumeralOrOnlyWordsAreInsufficient() {
        listOf("100 MALOTI", "ONE HUNDRED MALOTI", "MALOTI").forEach {
            assertEquals(CurrencyReadingStatus.INSUFFICIENT_EVIDENCE, LotiCurrencyReader.read("$issuer $it").status)
        }
    }

    @Test
    fun serialNumbersDoNotCountAsDenominationEvidence() {
        listOf("AB100200 CD100200", "100200 100200", "AB100 CD100", "100A 100B").forEach {
            assertEquals(CurrencyReadingStatus.INSUFFICIENT_EVIDENCE,
                LotiCurrencyReader.read("$issuer MALOTI $it").status)
        }
    }

    @Test
    fun amountsAndDatesCannotBeMistakenForRepeatedDenominations() {
        listOf("100.00 100.00", "100,000 100,000", "20/2025 20/2025", "-100 -100", "+100 +100")
            .forEach {
                assertEquals(CurrencyReadingStatus.INSUFFICIENT_EVIDENCE,
                    LotiCurrencyReader.read("$issuer MALOTI $it").status)
            }
    }

    @Test
    fun conflictingNumeralsAndWordsReturnNoValue() {
        listOf("20 50 MALOTI", "20 TWENTY MALOTI 100", "200 ONE HUNDRED MALOTI").forEach {
            val result = LotiCurrencyReader.read("$issuer $it")
            assertEquals(CurrencyReadingStatus.CONFLICTING_TEXT, result.status)
            assertNull(result.denomination)
        }
    }

    @Test
    fun unsupportedAmountsAreRejected() {
        assertEquals(CurrencyReadingStatus.CONFLICTING_TEXT,
            LotiCurrencyReader.read("$issuer 500 MALOTI 500").status)
    }

    @Test
    fun foreignCurrencyCannotBeReportedAsMaloti() {
        listOf("RAND", "SOUTH AFRICAN RESERVE BANK", "EMALANGENI", "DOLLARS").forEach {
            assertEquals(CurrencyReadingStatus.CONFLICTING_TEXT,
                LotiCurrencyReader.read("$issuer 20 MALOTI 20 $it").status)
        }
    }

    @Test
    fun doesNotGuessMisreadDigits() {
        assertEquals(CurrencyReadingStatus.INSUFFICIENT_EVIDENCE,
            LotiCurrencyReader.read("$issuer MALOTI 2O 2O").status)
    }
}
