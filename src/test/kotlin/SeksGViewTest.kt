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

    private val en = SeksG(600_000)
    private val to = SeksG(660_000)
    private val tre = SeksG(720_000)
    private val fire = SeksG(780_000)
    private val fem = SeksG(840_000)

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
        assertNull(finnObservasjon(SeksG(900_000)))
    }

    @Test
    fun `finn noe enkelt`() {
        dao.registrer(en, januar)
        assertEquals(Observasjon(en, januar, januar), finnObservasjon(en))
    }

    @Test
    fun `oppdater seneste observasjon`() {
        dao.registrer(to, januar)
        dao.registrer(to, februar)
        assertEquals(Observasjon(to, januar, februar), finnObservasjon(to))
    }

    @Test
    fun `oppdater tidligste observasjon`() {
        dao.registrer(tre, februar)
        dao.registrer(tre, januar)
        assertEquals(Observasjon(tre, januar, februar), finnObservasjon(tre))
    }

    @Test
    fun `oppdater i begge ender`() {
        dao.registrer(fire, februar)
        dao.registrer(fire, mars)
        dao.registrer(fire, januar)
        assertEquals(Observasjon(fire, januar, mars), finnObservasjon(fire))
    }

    @Test
    fun `ikke alt skal oppdatere noe`() {
        dao.registrer(fem, mars)
        assertEquals(Observasjon(fem, mars, mars), finnObservasjon(fem))

        dao.registrer(fem, januar)
        assertEquals(Observasjon(fem, januar, mars), finnObservasjon(fem))

        dao.registrer(fem, februar)
        assertEquals(Observasjon(fem, januar, mars), finnObservasjon(fem))
    }

    private fun AnvendtGrunnbeløpDao.registrer(seksG: SeksG, skjæringstidspunkt: LocalDate) =
        lagre(AnvendtGrunnbeløpDto(aktørId = "1", personidentifikator = "2", skjæringstidspunkt = skjæringstidspunkt, `6G`= seksG))

    private fun finnObservasjon(seksG: SeksG) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "select seks_g, tidligste_skjaeringstidspunkt, seneste_skjaeringstidspunkt from seks_g where seks_g = :seks_g"
        session.run(queryOf(statement, mapOf("seks_g" to seksG.verdi)).map { row ->
            Observasjon(
                SeksG(row.double("seks_g")),
                row.localDate("tidligste_skjaeringstidspunkt"),
                row.localDate("seneste_skjaeringstidspunkt")
            )}.asSingle
        )
    }

    private data class Observasjon(val seksG: SeksG, val tidligsteSkjaeringstidspunkt: LocalDate, val senesteSkjaeringstidspunkt: LocalDate)
}