import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtkastTilVedtakRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val testRapid = TestRapid().apply {
        UtkastTilVedtakRiver(this, anvendtGrunnbeløpDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.lagre(any()) }.answers {}
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

    @Test
    fun `lagrer ikke gammel moro`() {
        testRapid.sendTestMessage(event("utkast_til_vedtak", "2020-09-20"))
        verify(exactly = 0) {
            anvendtGrunnbeløpDao.lagre(any())
        }
    }


    @Test
    fun `lagrer fra og med da vi var ferdig med gammal moro`() {
        testRapid.sendTestMessage(event("utkast_til_vedtak", "2020-09-21"))
        verify(exactly = 1) {
            anvendtGrunnbeløpDao.lagre(any())
        }
    }

    @Language("JSON")
    private fun event(eventName: String, skjæringstidspunkt: String = "2024-01-01"): String = """{
        "@event_name": "$eventName",
        "fødselsnummer": "fødselsnummer",
        "skjæringstidspunkt": "$skjæringstidspunkt",
        "sykepengegrunnlagsfakta": {
          "6G": 666666.0
        }
    }"""
}
