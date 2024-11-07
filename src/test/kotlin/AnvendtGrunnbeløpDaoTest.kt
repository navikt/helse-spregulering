import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
            personidentifikator = "1B",
            skjæringstidspunkt = LocalDate.parse("2018-01-01"),
            `6G`= SeksG(600_000)
        )
        val anvendtGrunnbeløp2 = AnvendtGrunnbeløpDto(
            personidentifikator = "2B",
            skjæringstidspunkt = LocalDate.parse("2018-01-01"),
            `6G`= SeksG(600_000)
        )
        dao.lagre(anvendtGrunnbeløp1)
        assertEquals(listOf(anvendtGrunnbeløp1), hentAlle())
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp1, anvendtGrunnbeløp2), hentAlle())
    }

    @Test
    fun `finner feilanvendte grunnbeløp`() {
        val riktigGrunnbeløp = 150_000.0
        val feilGrunnbeløp = 300_000.0
        val grunnbeløpGjelderFra = LocalDate.parse("2018-01-01")

        val anvendtGrunnbeløp1 = AnvendtGrunnbeløpDto(
            personidentifikator = "1B",
            skjæringstidspunkt = grunnbeløpGjelderFra,
            `6G`= SeksG.fraGrunnbeløp(riktigGrunnbeløp)
        )
        val anvendtGrunnbeløp2 = AnvendtGrunnbeløpDto(
            personidentifikator = "2B",
            skjæringstidspunkt = grunnbeløpGjelderFra,
            `6G`= SeksG.fraGrunnbeløp(feilGrunnbeløp)
        )
        dao.lagre(anvendtGrunnbeløp1)
        dao.lagre(anvendtGrunnbeløp2)

        val feilanvendteGrunnbeløp = dao.hentFeilanvendteGrunnbeløp(
            Periode(grunnbeløpGjelderFra, LocalDate.MAX),
            SeksG.fraGrunnbeløp(riktigGrunnbeløp)
        )
        assertEquals(listOf(anvendtGrunnbeløp2), feilanvendteGrunnbeløp)
    }

    @Test
    fun `oppdaterer sykefraværstilfelle ved ny g`() = runBlocking {
        val skjæringstidspunkt = LocalDate.parse("2018-01-01")
        val personidentifikator = "1B"
        val anvendtGrunnbeløp1 = AnvendtGrunnbeløpDto(
            personidentifikator = personidentifikator,
            skjæringstidspunkt = skjæringstidspunkt,
            `6G`= SeksG(600_000)
        )
        val anvendtGrunnbeløp2 = anvendtGrunnbeløp1.copy(`6G`= SeksG(660_000))

        dao.lagre(anvendtGrunnbeløp1)
        assertEquals(listOf(anvendtGrunnbeløp1), hentAlle())
        val oppdatert1 = oppdatert(personidentifikator, skjæringstidspunkt)

        delay(1)
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp2), hentAlle())
        val oppdatert2 = oppdatert(personidentifikator, skjæringstidspunkt)
        assertNotEquals(oppdatert1, oppdatert2)

        delay(1)
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp2), hentAlle())
        val oppdatert3 = oppdatert(personidentifikator, skjæringstidspunkt)

        assertNotEquals(oppdatert2, oppdatert3)
        //assertEquals(oppdatert2, oppdatert3)
    }

    @Test
    fun `slette sykefraværstilfelle`() {
        val skjæringstidspunkt = LocalDate.parse("2018-01-01")
        val personidentifikator = "1B"
        val anvendtGrunnbeløp1 = AnvendtGrunnbeløpDto(
            personidentifikator = personidentifikator,
            skjæringstidspunkt = skjæringstidspunkt,
            `6G`= SeksG(600_000)
        )
        val anvendtGrunnbeløp2 = AnvendtGrunnbeløpDto(
            personidentifikator = personidentifikator,
            skjæringstidspunkt = LocalDate.parse("2019-01-01"),
            `6G`= SeksG(600_000)
        )
        dao.lagre(anvendtGrunnbeløp1)
        dao.lagre(anvendtGrunnbeløp2)
        assertEquals(listOf(anvendtGrunnbeløp1, anvendtGrunnbeløp2), hentAlle())

        dao.slettSykefraværstilfelle(personidentifikator, skjæringstidspunkt)
        assertEquals(listOf(anvendtGrunnbeløp2), hentAlle())
    }

    @Test
    fun `er det noen sykefraværstilfeller med feil grunnbeløp`() {
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
        dao.lagre(1_200_000.0, LocalDate.parse("2018-05-01"))
        dao.lagre(1_200_000.0, LocalDate.parse("2019-04-30"))
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
        dao.lagre(2_400_000.0, LocalDate.parse("2019-05-01"))
        dao.lagre(2_400_000.0, LocalDate.parse("2020-04-30"))
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
        dao.lagre(1_200_000.0, LocalDate.parse("2020-01-01"))
        dao.lagre(2_400_000.0, LocalDate.parse("2020-01-02"))
        val forventetPerioderMedForskjelligeGrunnbeløp = mapOf(
            Periode(LocalDate.parse("2019-05-01"), LocalDate.parse("2020-01-01")) to SeksG(2_400_000)
        )
        assertEquals(forventetPerioderMedForskjelligeGrunnbeløp, dao.perioderMedForskjelligGrunnbeløp())
        dao.lagre(2_400_000.0, LocalDate.parse("2020-01-01"))
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
    }

    @Test
    fun `dagens bilde`() {
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
        dao.lagre(711_720.0, LocalDate.parse("2023-05-01"))
        dao.lagre(711_720.0, LocalDate.parse("2024-04-30"))
        dao.lagre(744_168.0, LocalDate.parse("2024-05-01"))
        dao.lagre(744_168.0, LocalDate.parse("2024-08-19"))
        assertEquals(emptyMap<Periode, SeksG>(), dao.perioderMedForskjelligGrunnbeløp())
    }

    private fun hentAlle(): List<AnvendtGrunnbeløpDto> {
        return sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT * FROM anvendt_grunnbeloep").map { AnvendtGrunnbeløpDto(
                personidentifikator = it.string("personidentifikator"),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                `6G` = SeksG(it.double("seks_g")),
            ) }.asList)
        }
    }

    private fun oppdatert(personidentifikator: String, skjæringstidspunkt: LocalDate) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT oppdatert FROM anvendt_grunnbeloep WHERE personidentifikator = '$personidentifikator' AND skjaeringstidspunkt = '$skjæringstidspunkt'").map { it.localDateTime("oppdatert") }.asSingle)!!
    }

    private fun AnvendtGrunnbeløpDao.lagre(seksG: Double, skjæringstidspunkt: LocalDate) {
        lagre(AnvendtGrunnbeløpDto(
            personidentifikator = "2",
            skjæringstidspunkt = skjæringstidspunkt,
            `6G`= SeksG(seksG)
        ))
    }
}
