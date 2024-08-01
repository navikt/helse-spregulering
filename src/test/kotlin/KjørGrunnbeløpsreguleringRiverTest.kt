import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KjørGrunnbeløpsreguleringRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val testRapid = TestRapid().apply {
        KjørGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any(), any()) }.answers { emptyList() }
    }

    @Test
    fun `kjører grunnbeløpsregulering`() {
        testRapid.sendTestMessage(event("kjør_grunnbeløpsregulering", 101_000.0))
        verify(exactly = 1) {
            anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any(), any())
        }
    }

    @Test
    fun `kjører ikke grunnbeløpsregulering om noen er skikkelig på jordet med grunnbeløpet`() {
        testRapid.sendTestMessage(event("kjør_grunnbeløpsregulering", 1_000_000.0))
        verify(exactly = 0) {
            anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any(), any())
        }
    }

    @Test
    fun `kjører ikke grunnbeløpsregulering fra andre events`() {
        testRapid.sendTestMessage(event("tullete_kjøring", 101_000.0))
        verify(exactly = 0) {
            anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(any(), any(), any())
        }
    }

    @Language("JSON")
    private fun event(eventName: String, riktigGrunnbeløp: Double): String = """{
        "@event_name": "$eventName",
        "grunnbeløpGjelderFra": "2024-01-01",
        "riktigGrunnbeløp": $riktigGrunnbeløp
    }"""
}
