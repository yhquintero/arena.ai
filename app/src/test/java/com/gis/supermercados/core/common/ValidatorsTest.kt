package com.gis.supermercados.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de las validaciones de entrada. Son la primera linea de defensa contra datos
 * corruptos (importes negativos, SKUs duplicados, contraseñas debiles, rangos imposibles).
 */
class ValidatorsTest {

    @Test
    fun requiredRechazaTextosVacios() {
        assertTrue(Validators.required("Sucursal Centro").isValid)
        assertFalse(Validators.required("").isValid)
        assertFalse(Validators.required("   ").isValid)
        assertFalse(Validators.required(null).isValid)
    }

    @Test
    fun textAplicaLongitudMinimaYMaxima() {
        assertTrue(Validators.text("Arroz").isValid)
        assertFalse(Validators.text("A").isValid)
        assertFalse(Validators.text("x".repeat(121)).isValid)
    }

    @Test
    fun optionalTextAceptaVacioYPeroNoExcesos() {
        assertTrue(Validators.optionalText("").isValid)
        assertTrue(Validators.optionalText("Nota breve").isValid)
        assertFalse(Validators.optionalText("x".repeat(201)).isValid)
    }

    @Test
    fun emailValidaFormatoYAdmiteVacioSoloSiEsOpcional() {
        assertTrue(Validators.email("compras@mercado.com").isValid)
        assertFalse(Validators.email("no-es-correo").isValid)
        assertFalse(Validators.email("").isValid)
        assertTrue(Validators.email("", optional = true).isValid)
    }

    @Test
    fun phoneEsOpcionalPorDefecto() {
        assertTrue(Validators.phone("").isValid)
        assertTrue(Validators.phone("+53 5 1234567").isValid)
        assertFalse(Validators.phone("abc").isValid)
    }

    @Test
    fun usernameExigeLongitudYCaracteresPermitidos() {
        assertTrue(Validators.username("admin").isValid)
        assertTrue(Validators.username("cajero_01").isValid)
        assertFalse(Validators.username("ab").isValid)
        assertFalse(Validators.username("con espacio").isValid)
        assertFalse(Validators.username("").isValid)
    }

    @Test
    fun passwordExigeMinimoLetrasYDigitos() {
        assertTrue(Validators.password("Gis#2026").isValid)
        assertFalse(Validators.password("corta1").isValid)
        assertFalse(Validators.password("sololetras").isValid)
        assertFalse(Validators.password("12345678").isValid)
    }

    @Test
    fun passwordsMatchExigeCoincidencia() {
        assertTrue(Validators.passwordsMatch("Gis#2026", "Gis#2026").isValid)
        assertFalse(Validators.passwordsMatch("Gis#2026", "Otra#2026").isValid)
        assertFalse(Validators.passwordsMatch("", "").isValid)
    }

    @Test
    fun skuAceptaCodigoAlfanumericoConGuionesYPuntos() {
        assertTrue(Validators.sku("ARROZ-5KG").isValid)
        assertTrue(Validators.sku("A.12_3").isValid)
        assertFalse(Validators.sku("AB").isValid)
        assertFalse(Validators.sku("con espacio").isValid)
    }

    @Test
    fun barcodeEsOpcionalPeroValidaCuandoSeInforma() {
        assertTrue(Validators.barcode("").isValid)
        assertTrue(Validators.barcode("7501234567890").isValid)
        assertFalse(Validators.barcode("12").isValid)
        assertFalse(Validators.barcode("ab", optional = false).isValid)
    }

    @Test
    fun moneyAceptaFormatosComunesYRechazaNegativos() {
        assertTrue(Validators.money("1.234,56").isValid)
        assertTrue(Validators.money("12").isValid)
        assertTrue(Validators.money("0").isValid)
        assertFalse(Validators.money("0", allowZero = false).isValid)
        assertFalse(Validators.money("").isValid)
        assertFalse(Validators.money("abc").isValid)
        assertFalse(Validators.money("-50").isValid)
        assertFalse(Validators.money("99999999999999").isValid)
    }

    @Test
    fun positiveIntRechazaDecimalesYNegativos() {
        assertTrue(Validators.positiveInt("10").isValid)
        assertTrue(Validators.positiveInt("0").isValid)
        assertFalse(Validators.positiveInt("0", allowZero = false).isValid)
        assertFalse(Validators.positiveInt("-3").isValid)
        assertFalse(Validators.positiveInt("2,5").isValid)
        assertFalse(Validators.positiveInt("").isValid)
    }

    @Test
    fun percentLimitaElMaximo() {
        assertTrue(Validators.percent("16").isValid)
        assertTrue(Validators.percent("100").isValid)
        assertFalse(Validators.percent("101").isValid)
        assertFalse(Validators.percent("-1").isValid)
        assertFalse(Validators.percent("abc").isValid)
    }

    @Test
    fun timeOfDayValidaFormatoHHmm() {
        assertTrue(Validators.timeOfDay("08:00").isValid)
        assertTrue(Validators.timeOfDay("23:59").isValid)
        assertFalse(Validators.timeOfDay("24:00").isValid)
        assertFalse(Validators.timeOfDay("8:00").isValid)
        assertFalse(Validators.timeOfDay("").isValid)
    }

    @Test
    fun dateRangeExigeInicioAnteriorAlFin() {
        val start = 1_700_000_000_000L
        assertTrue(Validators.dateRange(start, start + 86_400_000L).isValid)
        assertTrue(Validators.dateRange(start, start).isValid)
        assertFalse(Validators.dateRange(start + 86_400_000L, start).isValid)
    }

    @Test
    fun firstErrorDevuelveElPrimerFalloEnOrden() {
        val ok = Validators.required("dato")
        val bad = Validators.required("")

        assertTrue(Validators.firstError(ok, ok).isValid)
        assertFalse(Validators.firstError(ok, bad).isValid)
        assertEquals(bad.errorText, Validators.firstError(bad, ok).errorText)
    }
}
