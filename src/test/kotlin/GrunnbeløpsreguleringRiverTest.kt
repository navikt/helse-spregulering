import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnbeløpsreguleringRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val testRapid = TestRapid().apply {
        ManuellGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
        AutomatiskGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }.answers { emptyList() }
        every { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }.answers { false }
    }

    @Test
    fun `kjører manuell grunnbeløpsregulering`() {
        testRapid.sendTestMessage(manueltEvent(101_000.0))
        verify(exactly = 1) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }
    }

    @Test
    fun `kjører ikke manuell grunnbeløpsregulering med ugyldig periode`() {
        testRapid.sendTestMessage(manueltEvent(101_000.0, tom = LocalDate.parse("2023-01-01")))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }
    }

    @Test
    fun `kjører ikke manuell grunnbeløpsregulering om noen er skikkelig på jordet med grunnbeløpet`() {
        testRapid.sendTestMessage(manueltEvent(1_000_000.0))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }
    }

    @Test
    fun `kjører 'automatisk' grunnbeløpsregulering som er sparket igang manuelt`() {
        testRapid.sendTestMessage(event("kjør_grunnbeløpsregulering"))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 1) { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }
    }

    @Test
    fun `kjører automatisk grunnbeløpsregulering hver time`() {
        testRapid.sendTestMessage(event("hel_time"))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 1) { anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp() }
    }

    @Language("JSON")
    private fun manueltEvent(riktigGrunnbeløp: Double, tom: LocalDate? = null): String = """{
        "@event_name": "kjør_grunnbeløpsregulering",
        "grunnbeløpGjelderFra": "2024-01-01",
        "grunnbeløpGjelderTil": ${tom?.let { "\"$it\"" }},
        "riktigGrunnbeløp": $riktigGrunnbeløp
    }"""

    @Language("JSON")
    private fun event(navn: String): String = """{ "@event_name": "$navn" }"""
}
