import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.LocalDate.MIN

class GrunnbeløpsreguleringRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val testRapid = TestRapid().apply {
        ManuellGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
        AutomatiskGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
    }
    private val perioderMedFeilGrunnbeløp = mapOf(Periode(MIN, MIN) to SeksG(600_000), Periode(MAX, MAX) to SeksG(660_000))
    private val feilanvendtGrunnbeløp = listOf(AnvendtGrunnbeløpDto("1", LocalDate.parse("2018-01-01"), SeksG(720_000)))

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }.answers { feilanvendtGrunnbeløp }
        every { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }.answers { perioderMedFeilGrunnbeløp }
    }

    @Test
    fun `kjører manuell grunnbeløpsregulering`() {
        testRapid.sendTestMessage(manueltEvent(101_000.0))
        verify(exactly = 1) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }
        assertEquals(2, testRapid.inspektør.size)
        assertMeldingstype(0, "grunnbeløpsregulering")
        assertMeldingstype(1, "slackmelding")
    }

    @Test
    fun `kjører ikke manuell grunnbeløpsregulering med ugyldig periode`() {
        testRapid.sendTestMessage(manueltEvent(101_000.0, tom = LocalDate.parse("2023-01-01")))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `kjører ikke manuell grunnbeløpsregulering om noen er skikkelig på jordet med grunnbeløpet`() {
        testRapid.sendTestMessage(manueltEvent(1_000_000.0))
        verify(exactly = 0) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 0) { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `kjører 'automatisk' grunnbeløpsregulering som er sparket igang manuelt`() {
        testRapid.sendTestMessage(event("kjør_grunnbeløpsregulering"))
        verify(exactly = 2) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 1) { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }
        assertEquals(3, testRapid.inspektør.size)
        assertMeldingstype(0, "grunnbeløpsregulering")
        assertMeldingstype(1, "grunnbeløpsregulering")
        assertMeldingstype(2, "slackmelding")
    }

    @Test
    fun `kjører automatisk grunnbeløpsregulering hver time`() {
        testRapid.sendTestMessage(event("hel_time"))
        verify(exactly = 2) { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any()) }
        verify(exactly = 1) { anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp() }
        assertEquals(3, testRapid.inspektør.size)
        assertMeldingstype(0, "grunnbeløpsregulering")
        assertMeldingstype(1, "grunnbeløpsregulering")
        assertMeldingstype(2, "slackmelding")
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

    private fun assertMeldingstype(index: Int, forventet: String) =
        assertEquals(forventet,testRapid.inspektør.message(index).path("@event_name").asText())
}
