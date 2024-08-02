import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtkastTilVedtakRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val seksGDao: SeksGDao = mockk()
    private val testRapid = TestRapid().apply {
        UtkastTilVedtakRiver(this, anvendtGrunnbeløpDao, seksGDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.lagre(any()) }.answers {}
        every { seksGDao.registrer(any(), any()) }.answers {}
    }

    @Test
    fun `lagrer data fra utkast_til_vedtak`() {
        testRapid.sendTestMessage(event("utkast_til_vedtak"))
        verify(exactly = 1) {
            anvendtGrunnbeløpDao.lagre(any())
        }
    }

    @Test
    fun `lagrer ikke data fra andre events`() {
        testRapid.sendTestMessage(event("lol"))
        verify(exactly = 0) {
            anvendtGrunnbeløpDao.lagre(any())
        }
    }

    @Language("JSON")
    private fun event(eventName: String): String = """{
        "@event_name": "$eventName",
        "fødselsnummer": "fødselsnummer",
        "aktørId": "aktørId",
        "skjæringstidspunkt": "2024-01-01",
        "sykepengegrunnlagsfakta": {
          "6G": 666666.0
        }
    }"""
}
