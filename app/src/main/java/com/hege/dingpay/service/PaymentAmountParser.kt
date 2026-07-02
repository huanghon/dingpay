package com.hege.dingpay.service

import com.hege.dingpay.data.ParsedPayment

object PaymentAmountParser {
    // Numeric part accepts thousand separators and decimals.
    // Two branches:
    //   1) numbers with mandatory thousand grouping: 1,234 / 1.234 / 1,234.56 / 1.234,56
    //   2) plain numbers with optional decimals: 1234 / 1.25 / 1,25
    private const val NUMBER =
        """(?:\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)"""

    private val patterns: List<Regex> = listOf(
        // Prefixed currency, e.g. USD 1,234.56 / US$ 1.25 / EUR 1.234,56 / € 1,25 / PAB 1.25 / B/. 1.25
        Regex("""(?i)(USD|US\$|EUR|€|PAB|B/\.|\$)\s*[+]?\s*($NUMBER)"""),
        // Suffix currency, e.g. 1.25 USD / 1,25 € / 1,234.56 EUR / 1.25 B/.
        Regex("""(?i)[+]?($NUMBER)\s*(USD|US\$|EUR|€|PAB|B/\.|\$)""")
    )

    fun parse(rawText: String): ParsedPayment? {
        for (pattern in patterns) {
            val match = pattern.find(rawText) ?: continue
            val groups = match.groupValues
            // Locate the numeric group and the currency token independently of order.
            val currencyToken = groups.drop(1).firstOrNull { it.isNotBlank() && !it.any(Char::isDigit) }
                ?: continue
            val amountText = groups.drop(1).firstOrNull { it.any(Char::isDigit) } ?: continue
            val amount = normalizeAmount(amountText) ?: continue
            val currency = currencyOf(currencyToken)
            return ParsedPayment(amount = amount, currency = currency)
        }
        return null
    }

    /**
     * Handles thousand separators for both `1,234.56` (English) and `1.234,56`
     * (continental European) styles, plus plain forms like `1,25` and `1.25`.
     * Returns null if the text cannot be interpreted as a number.
     */
    internal fun normalizeAmount(raw: String): Double? {
        val cleaned = raw.replace("+", "").trim()
        if (cleaned.isEmpty()) return null

        val hasDot = cleaned.contains('.')
        val hasComma = cleaned.contains(',')

        val normalized = when {
            hasDot && hasComma -> {
                // Last separator is the decimal separator; the other is a thousand grouping.
                val lastDot = cleaned.lastIndexOf('.')
                val lastComma = cleaned.lastIndexOf(',')
                if (lastComma > lastDot) {
                    // European style: 1.234,56 -> strip dots, comma -> dot.
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // English style: 1,234.56 -> strip commas.
                    cleaned.replace(",", "")
                }
            }
            hasComma -> {
                val decimals = cleaned.substringAfterLast(',')
                if (decimals.length in 1..2) {
                    // Treat comma as decimal separator: 1,25 -> 1.25.
                    cleaned.replace(',', '.')
                } else {
                    // Treat comma as thousand separator: 1,234 -> 1234.
                    cleaned.replace(",", "")
                }
            }
            hasDot -> {
                val decimals = cleaned.substringAfterLast('.')
                if (decimals.length in 1..2) {
                    // Treat dot as decimal separator: 1.25 -> 1.25.
                    cleaned
                } else {
                    // Treat dot as thousand separator: 1.234 -> 1234.
                    cleaned.replace(".", "")
                }
            }
            else -> cleaned
        }
        return normalized.toDoubleOrNull()
    }

    private fun currencyOf(token: String): String {
        val upper = token.trim().uppercase()
        return when {
            upper == "EUR" || upper == "€" -> "€"
            upper == "PAB" || upper == "B/." -> "B/."
            upper == "USD" || upper == "US\$" || upper == "$" -> "$"
            else -> "$"
        }
    }
}
