import Periode.Companion.overlappendePerioder
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class AnvendtGrunnbeløpDao(private val dataSource: DataSource) {
    fun lagre(anvendtGrunnbeløpDto: AnvendtGrunnbeløpDto) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO anvendt_grunnbeloep (personidentifikator, skjaeringstidspunkt, seks_g) 
            VALUES (:personidentifikator, :skjaeringstidspunkt, :seks_g)
            ON CONFLICT(personidentifikator, skjaeringstidspunkt) DO UPDATE SET seks_g=:seks_g, oppdatert=now()
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "personidentifikator" to anvendtGrunnbeløpDto.personidentifikator,
                "skjaeringstidspunkt" to anvendtGrunnbeløpDto.skjæringstidspunkt,
                "seks_g" to anvendtGrunnbeløpDto.`6G`.verdi
            )).asExecute)
        }
    }

    fun hentFeilanvendteGrunnbeløp(
        periode: Periode,
        riktigSeksG: SeksG
    ): List<AnvendtGrunnbeløpDto> {
        @Language("PostgreSQL")
        val statement = """
            SELECT * FROM anvendt_grunnbeloep
            WHERE skjaeringstidspunkt >= :grunnbeloep_gjelder_fra
            AND skjaeringstidspunkt <= :grunnbeloep_gjelder_til
            AND seks_g != :riktig_seks_g
        """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "grunnbeloep_gjelder_fra" to periode.start.postgresifiser,
                "grunnbeloep_gjelder_til" to periode.endInclusive.postgresifiser,
                "riktig_seks_g" to riktigSeksG.verdi
            )).map { AnvendtGrunnbeløpDto(
                personidentifikator = it.string("personidentifikator"),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                `6G` = SeksG(it.double("seks_g")),
            ) }.asList)
        }
    }

    fun slettSykefraværstilfelle(personidentifikator: String, skjæringstidspunkt: LocalDate) {
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM anvendt_grunnbeloep 
            WHERE personidentifikator = :personidentifikator 
            AND skjaeringstidspunkt = :skjaeringstidspunkt
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "personidentifikator" to personidentifikator,
                "skjaeringstidspunkt" to skjæringstidspunkt
            )).asExecute)
        }
    }

    fun perioderMedForskjelligGrunnbeløp(): Map<Periode, SeksG> {
        @Language("PostgreSQL")
        val statement = """
            SELECT * FROM seks_g
            WHERE seks_g >= $SeksG2023
        """
        val grunnbeløp = sessionOf(dataSource).use { session ->
            session.run(queryOf(statement).map { row ->
                val periode = Periode(row.localDate("tidligste_skjaeringstidspunkt"), row.localDate("seneste_skjaeringstidspunkt"))
                val seksG = SeksG(row.double("seks_g"))
                seksG to periode
            }.asList).toMap()
        }

        // Finner alle perioder som har forskjellig grunnbeløp og kobler de mot det største grunnbeløpet, som det er naturlig å tro at er det riktige.
        return grunnbeløp.values.overlappendePerioder().associateWith { periodeMedForskjelligeGrunnbeløp ->
            grunnbeløp.filterValues { periodeMedForskjelligeGrunnbeløp.overlapperMed(it) }.maxOf { (seksG, _) -> seksG }
        }
    }

    private companion object {
        /**
         * Hvorfor akkurat denne spør du?
         * - Vi G-regulerte 2023 og 2024, så fra og med 1.Mai 2023 så skal alt være rett,
         *   og eventuelle observasjoner etter dette er "feil" grunnbeløp på sykefraværstilfeller.
         */
        private const val SeksG2023 = 711720.0

        private val Minish = LocalDate.parse("0000-01-01")
        private val Maxish = LocalDate.parse("9999-12-31")
        private val LocalDate.postgresifiser get() = coerceAtLeast(Minish).coerceAtMost(Maxish)
    }
}