import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SeksGViewTest {
    private lateinit var testDataSource: TestDataSource
    private val dataSource get() = testDataSource.ds
    private val dao get() = AnvendtGrunnbeløpDao(dataSource)
    private val januar = LocalDate.of(2025, 1, 1)
    private val februar = LocalDate.of(2025, 2, 1)
    private val mars = LocalDate.of(2025, 3, 1)

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
    fun `ikke finn noe`() {
        assertNull(finnObservasjon(12.3))
    }

    @Test
    fun `finn noe enkelt`() {
        dao.registrer(1.0, januar)
        assertEquals(Observasjon(1.0, januar, januar), finnObservasjon(1.0))
    }

    @Test
    fun `oppdater seneste observasjon`() {
        dao.registrer(2.0, januar)
        dao.registrer(2.0, februar)
        assertEquals(Observasjon(2.0, januar, februar), finnObservasjon(2.0))
    }

    @Test
    fun `oppdater tidligste observasjon`() {
        dao.registrer(3.0, februar)
        dao.registrer(3.0, januar)
        assertEquals(Observasjon(3.0, januar, februar), finnObservasjon(3.0))
    }

    @Test
    fun `oppdater i begge ender`() {
        dao.registrer(4.0, februar)
        dao.registrer(4.0, mars)
        dao.registrer(4.0, januar)
        assertEquals(Observasjon(4.0, januar, mars), finnObservasjon(4.0))
    }

    @Test
    fun `ikke alt skal oppdatere noe`() {
        dao.registrer(5.0, mars)
        assertEquals(Observasjon(5.0, mars, mars), finnObservasjon(5.0))

        dao.registrer(5.0, januar)
        assertEquals(Observasjon(5.0, januar, mars), finnObservasjon(5.0))

        dao.registrer(5.0, februar)
        assertEquals(Observasjon(5.0, januar, mars), finnObservasjon(5.0))
    }

    private fun AnvendtGrunnbeløpDao.registrer(seksG: Double, skjæringstidspunkt: LocalDate) =
        lagre(AnvendtGrunnbeløpDto(aktørId = "1", personidentifikator = "2", skjæringstidspunkt = skjæringstidspunkt, `6G`= seksG))

    private fun finnObservasjon(seksG: Double) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "select seks_g, tidligste_skjaeringstidspunkt, seneste_skjaeringstidspunkt from seks_g where seks_g = :seks_g"
        session.run(queryOf(statement, mapOf("seks_g" to seksG)).map { row ->
            Observasjon(
                row.double("seks_g"),
                row.localDate("tidligste_skjaeringstidspunkt"),
                row.localDate("seneste_skjaeringstidspunkt")
            )}.asSingle
        )
    }

    private data class Observasjon(val seksG: Double, val tidligsteSkjaeringstidspunkt: LocalDate, val senesteSkjaeringstidspunkt: LocalDate)
}