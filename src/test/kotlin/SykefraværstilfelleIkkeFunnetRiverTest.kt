import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SykefraværstilfelleIkkeFunnetRiverTest {

    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao = mockk()
    private val testRapid = TestRapid().apply {
        SykefraværstilfelleIkkeFunnetRiver(this, anvendtGrunnbeløpDao)
    }

    @BeforeEach
    fun setup() {
        every { anvendtGrunnbeløpDao.slettSykefraværstilfelle(any(), any()) }.answers { }
    }

    @Test
    fun `sletter sykefraværstilfelle`() {
        testRapid.sendTestMessage(event("sykefraværstilfelle_ikke_funnet"))
        verify(exactly = 1) {
            anvendtGrunnbeløpDao.slettSykefraværstilfelle(any(), any())
        }
    }

    @Test
    fun `sletter ikke sykefraværstilfelle fra andre events`() {
        testRapid.sendTestMessage(event("tullete_sykefraærstilfelle"))
        verify(exactly = 0) {
            anvendtGrunnbeløpDao.slettSykefraværstilfelle(any(), any())
        }
    }

    @Language("JSON")
    private fun event(eventName: String): String = """{
        "@event_name": "$eventName",
        "skjæringstidspunkt": "2024-01-01",
        "fødselsnummer": "1"
    }"""
}
