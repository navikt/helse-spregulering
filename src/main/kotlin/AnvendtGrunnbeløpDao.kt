import Periode.Companion.overlapper
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class AnvendtGrunnbeløpDao(private val dataSource: DataSource) {
    fun lagre(anvendtGrunnbeløpDto: AnvendtGrunnbeløpDto) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO anvendt_grunnbeloep (aktor_id, personidentifikator, skjaeringstidspunkt, seks_g) 
            VALUES (:aktor_id, :personidentifikator, :skjaeringstidspunkt, :seks_g)
            ON CONFLICT(personidentifikator, skjaeringstidspunkt) DO UPDATE SET seks_g=:seks_g, oppdatert=now()
        """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "aktor_id" to anvendtGrunnbeløpDto.aktørId,
                "personidentifikator" to anvendtGrunnbeløpDto.personidentifikator,
                "skjaeringstidspunkt" to anvendtGrunnbeløpDto.skjæringstidspunkt,
                "seks_g" to anvendtGrunnbeløpDto.`6G`
            )).asExecute)
        }
    }

    fun hentFeilanvendteGrunnbeløp(
        grunnbeløpGjelderFra: LocalDate,
        grunnbeløpGjelderTil: LocalDate,
        riktigGrunnbeløp: Double
    ): List<AnvendtGrunnbeløpDto> {
        val riktigSeksG = riktigGrunnbeløp * 6
        @Language("PostgreSQL")
        val statement = """
            SELECT * FROM anvendt_grunnbeloep
            WHERE skjaeringstidspunkt >= :grunnbeloep_gjelder_fra
            AND skjaeringstidspunkt <= :grunnbeloep_gjelder_til
            AND seks_g != :riktig_seks_g
        """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "grunnbeloep_gjelder_fra" to grunnbeløpGjelderFra.postgresifiser,
                "grunnbeloep_gjelder_til" to grunnbeløpGjelderTil.postgresifiser,
                "riktig_seks_g" to riktigSeksG
            )).map { AnvendtGrunnbeløpDto(
                aktørId = it.string("aktor_id"),
                personidentifikator = it.string("personidentifikator"),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                `6G` = it.double("seks_g"),
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

    fun erDetNoenSykefraværstilfellerMedFeilGrunnbeløp(): Boolean {
        @Language("PostgreSQL")
        val statement = """
            SELECT * FROM seks_g
            WHERE seks_g >= $SeksG2023
        """
        val perioderMedUnikeGrunnbeløp = sessionOf(dataSource).use { session ->
            session.run(queryOf(statement).map { row ->
                Periode(row.localDate("tidligste_skjaeringstidspunkt"), row.localDate("seneste_skjaeringstidspunkt"))
            }.asList)
        }

        // Om vi finner overlappende perioder på tvers av grunnbeløp så tyder det på at vi har brukt feil grunnbeløp
        return perioderMedUnikeGrunnbeløp.overlapper()
    }

    private companion object {
        /**
         * Hvorfor akkurat denne spør du?
         * - Vi G-regulerte 2023 og 2024, så fra og med 1.Mai 2023 så skal alt være rett,
         *   og eventuelle observasjoner etter dette er "feil" grunnbeløp.
         */
        private const val SeksG2023 = 711720.0

        private val Minish = LocalDate.parse("0000-01-01")
        private val Maxish = LocalDate.parse("9999-12-31")
        private val LocalDate.postgresifiser get() = coerceAtLeast(Minish).coerceAtMost(Maxish)
    }
}