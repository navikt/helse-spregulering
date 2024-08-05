import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MigrerteGrunnbeløpRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val seksGDao: SeksGDao = mockk()
    private val testRapid = TestRapid().apply {
        MigrerteGrunnbeløpRiver(this, anvendtGrunnbeløpDao, seksGDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.lagre(any()) }.answers {}
        every { seksGDao.registrer(any(), any()) }.answers {}
    }

    @Test
    fun `lagrer data fra grunnbeløp-melding`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 2) {
            anvendtGrunnbeløpDao.lagre(any())
        }
    }

    @Language("JSON")
    private fun event(): String = """
        {
          "fødselsnummer": "1",
          "aktørId": "2",
          "grunnbeløp": [
            {
              "skjæringstidspunkt": "2024-06-19",
              "6G": 744168.0
            },
            {
              "skjæringstidspunkt": "2023-06-19",
              "6G": 500123.0
            }
          ],
          "@event_name": "grunnbeløp"
        }
    """
}
