import SeksGDao.*
import com.github.navikt.tbd_libs.test_support.TestDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SeksGDaoTest {

    private lateinit var testDataSource: TestDataSource
    private val dataSource get() = testDataSource.ds
    private val dao get() = SeksGDao(dataSource)
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
        assertNull(dao.finnObservasjon(12.3))
    }

    @Test
    fun `finn noe enkelt`() {
        dao.registrer(1.0, januar)
        assertEquals(Observasjon(1.0, januar, januar), dao.finnObservasjon(1.0))
    }

    @Test
    fun `oppdater seneste observasjon`() {
        dao.registrer(2.0, januar)
        dao.registrer(2.0, februar)
        assertEquals(Observasjon(2.0, januar, februar), dao.finnObservasjon(2.0))
    }

    @Test
    fun `oppdater tidligste observasjon`() {
        dao.registrer(3.0, februar)
        dao.registrer(3.0, januar)
        assertEquals(Observasjon(3.0, januar, februar), dao.finnObservasjon(3.0))
    }

    @Test
    fun `oppdater i begge ender`() {
        dao.registrer(4.0, februar)
        dao.registrer(4.0, mars)
        dao.registrer(4.0, januar)
        assertEquals(Observasjon(4.0, januar, mars), dao.finnObservasjon(4.0))
    }

    @Test
    fun `ikke alt skal oppdatere noe`() {
        dao.registrer(5.0, mars)
        assertEquals(Observasjon(5.0, mars, mars), dao.finnObservasjon(5.0))

        dao.registrer(5.0, januar)
        assertEquals(Observasjon(5.0, januar, mars), dao.finnObservasjon(5.0))

        dao.registrer(5.0, februar)
        assertEquals(Observasjon(5.0, januar, mars), dao.finnObservasjon(5.0))
    }

}