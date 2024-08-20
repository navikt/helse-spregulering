import Periode.Companion.overlappendePerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeriodeTest {

    @Test
    fun `finner overlappende perioder`() {
        val en = "2020-05-01 - 2021-04-30".periode
        val to = "2021-02-28 - 2021-03-05".periode
        val tre = "2021-04-01 - 2021-04-05".periode
        val fire = "2021-04-05 - 2021-12-31".periode
        val fem = "2021-03-01 - 2021-03-01".periode

        val overlappendePerioder = listOf(en, to, tre, fire, fem).overlappendePerioder()
        assertEquals(overlappendePerioder, listOf(fem, fire, tre, to, en).overlappendePerioder())
        assertEquals(overlappendePerioder, listOf(fire, en, tre, to, fem).overlappendePerioder())

        val forventetOverlappndePerioder = listOf("2021-02-28 - 2021-03-05".periode, "2021-04-05 - 2021-04-05".periode)
        assertEquals(forventetOverlappndePerioder, overlappendePerioder)

        assertEquals(emptyList<Periode>(), listOf(to, tre).overlappendePerioder())
    }

    private companion object {
        private val String.periode get() = split(" - ").let { Periode(LocalDate.parse(it[0]), LocalDate.parse(it[1]))  }
    }
}