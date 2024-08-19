import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeksGTest {

    @Test
    fun `6G som ikke er delelig på 6 kan umulig væra rett`() {
        assertEquals(
            "124028.33333333333 virker jo som et rart grunnbeløp da. 6G er 744170.0, virkelig?",
            assertThrows<IllegalArgumentException> { SeksG(744170.0) }.message
        )
    }

    @Test
    fun `Kjente grunnbeløp håndteres`() {
        KjenteGrunnbeløp.forEach { (_, grunnbeløp) ->
            val forventet = grunnbeløp * 6
            val fra1G = SeksG.fraGrunnbeløp(grunnbeløp)
            val fra6G = SeksG(forventet)
            assertEquals(forventet, fra1G.verdi)
            assertEquals(forventet, fra6G.verdi)
            assertEquals(fra1G, fra6G)
        }
    }

    @Test
    fun `Nye grunnbeløp håndteres`() {
        val potensieltFremtidigGrunnbeløp = KjenteGrunnbeløp.getValue(2019) + KjenteGrunnbeløp.getValue(2020)
        val forventet = potensieltFremtidigGrunnbeløp * 6
        val fra1G = SeksG.fraGrunnbeløp(potensieltFremtidigGrunnbeløp)
        val fra6G = SeksG(forventet)
        assertEquals(forventet, fra1G.verdi)
        assertEquals(forventet, fra6G.verdi)
        assertEquals(fra1G, fra6G)
    }

    @Test
    fun `Å sende inn 6G som 1G skal feile`(){
        KjenteGrunnbeløp.mapValues { (_, grunnbeløp) -> grunnbeløp * 6 }.forEach { (_, seks6) ->
            assertEquals(
                assertThrows<IllegalArgumentException> { SeksG.fraGrunnbeløp(seks6) }.message,
                "$seks6 virker jo som et rart grunnbeløp da. 6G er ${seks6 * 6}, virkelig?"
            )
        }
    }

    private companion object {
        private val KjenteGrunnbeløp = mapOf(
            2019 to 99858.0,
            2020 to 101351.0,
            2021 to 106399.0,
            2022 to 111477.0,
            2023 to 118620.0,
            2024 to 124028.0
        )
    }
}