package com.hege.dingpay.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentAmountParserTest {

    @Test
    fun parses_usd_prefix() {
        val parsed = PaymentAmountParser.parse("You received $1.25 today")
        assertNotNull(parsed)
        assertEquals(1.25, parsed!!.amount, 0.0001)
        assertEquals("$", parsed.currency)
    }

    @Test
    fun parses_usd_prefix_with_plus() {
        val parsed = PaymentAmountParser.parse("Recibiste +$1,25")
        assertNotNull(parsed)
        assertEquals(1.25, parsed!!.amount, 0.0001)
        assertEquals("$", parsed.currency)
    }

    @Test
    fun parses_pab_balboa() {
        val parsed = PaymentAmountParser.parse("Recibiste B/. 1.25 de Juan")
        assertNotNull(parsed)
        assertEquals(1.25, parsed!!.amount, 0.0001)
        assertEquals("B/.", parsed.currency)
    }

    @Test
    fun parses_pab_word_prefix() {
        val parsed = PaymentAmountParser.parse("PAB 1234.56 acreditado")
        assertNotNull(parsed)
        assertEquals(1234.56, parsed!!.amount, 0.0001)
        assertEquals("B/.", parsed.currency)
    }

    @Test
    fun parses_eur_symbol_prefix() {
        val parsed = PaymentAmountParser.parse("Has recibido €1,25")
        assertNotNull(parsed)
        assertEquals(1.25, parsed!!.amount, 0.0001)
        assertEquals("€", parsed.currency)
    }

    @Test
    fun parses_eur_word_suffix_with_european_grouping() {
        val parsed = PaymentAmountParser.parse("Pago recibido 1.234,56 EUR")
        assertNotNull(parsed)
        assertEquals(1234.56, parsed!!.amount, 0.0001)
        assertEquals("€", parsed.currency)
    }

    @Test
    fun parses_usd_with_english_grouping() {
        val parsed = PaymentAmountParser.parse("Total USD 1,234.56 depositado")
        assertNotNull(parsed)
        assertEquals(1234.56, parsed!!.amount, 0.0001)
        assertEquals("$", parsed.currency)
    }

    @Test
    fun parses_us_dollar_prefix() {
        val parsed = PaymentAmountParser.parse("Charge US$ 42.00 today")
        assertNotNull(parsed)
        assertEquals(42.0, parsed!!.amount, 0.0001)
        assertEquals("$", parsed.currency)
    }

    @Test
    fun returns_null_when_no_currency() {
        assertNull(PaymentAmountParser.parse("Hola mundo sin dinero"))
    }

    @Test
    fun returns_null_when_only_currency_word() {
        assertNull(PaymentAmountParser.parse("USD sin monto"))
    }

    @Test
    fun normalize_amount_english_grouping() {
        assertEquals(1234.56, PaymentAmountParser.normalizeAmount("1,234.56")!!, 0.0001)
    }

    @Test
    fun normalize_amount_european_grouping() {
        assertEquals(1234.56, PaymentAmountParser.normalizeAmount("1.234,56")!!, 0.0001)
    }

    @Test
    fun normalize_amount_small_comma_decimal() {
        assertEquals(1.25, PaymentAmountParser.normalizeAmount("1,25")!!, 0.0001)
    }

    @Test
    fun normalize_amount_thousands_comma_only() {
        assertEquals(1234.0, PaymentAmountParser.normalizeAmount("1,234")!!, 0.0001)
    }
}
