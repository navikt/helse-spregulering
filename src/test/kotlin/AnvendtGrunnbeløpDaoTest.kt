import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

val databaseContainer = DatabaseContainers.container("spregulering", CleanupStrategy.tables("anvendt_grunnbeloep"))

class AnvendtGrunnbeløpDaoTest {
    private lateinit var testDataSource: TestDataSource
    private val dataSource get() = testDataSource.ds
    private val dao get() = AnvendtGrunnbeløpDao(dataSource)

    @BeforeEach
    fun setup() {
        testDataSource = databaseContainer.nyTilkobling()
        testDataSource.ds // Dette kjører migreringer
    }

    @AfterEach
    fun `stop postgres`() {
        databaseContainer.droppTilkobling(testDataSource)
    }

    @Test
    fun `lagrer anvendte grunnbeløp`() {
        val anvendtGrunnbeløp1 = AnvendtGrunnbeløpDto(
            aktørId = "1A",
            personidentifikator = "1B",
            skjæringstidspunkt = LocalDate.parse("2018-01-01"),
            `6G`= 123123.3
        )
        val anvendtGrunnbeløp2 = AnvendtGrunnbeløpDto(
            aktørId = "2A",
            personidentifikator = "2B",
            skjæringstidspunkt = LocalDate.parse("2018-01-01"),
            `6G`= 123123.3
        )
        dao.lagre(anvendtGrunnbeløp1)
        assertEquals(listOf(anvendtGrunnbeløp1), hentAlle())
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp1, anvendtGrunnbeløp2), hentAlle())
    }

    @Test
    fun `oppdaterer sykefraværstilfelle ved ny g`() {
        val anvendtGrunnbeløp1 = AnvendtGrunnbeløpDto(
            aktørId = "1A",
            personidentifikator = "1B",
            skjæringstidspunkt = LocalDate.parse("2018-01-01"),
            `6G`= 123123.3
        )
        val anvendtGrunnbeløp2 = anvendtGrunnbeløp1.copy(`6G`= 222222.0)

        dao.lagre(anvendtGrunnbeløp1)
        assertEquals(listOf(anvendtGrunnbeløp1), hentAlle())
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp2), hentAlle())
    }

    private fun hentAlle(): List<AnvendtGrunnbeløpDto> {
        return sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT * FROM anvendt_grunnbeloep").map { AnvendtGrunnbeløpDto(
                aktørId = it.string("aktor_id"),
                personidentifikator = it.string("personidentifikator"),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                `6G` = it.double("seks_g"),
            ) }.asList)
        }
    }
}
