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
                "grunnbeloep_gjelder_fra" to grunnbeløpGjelderFra,
                "grunnbeloep_gjelder_til" to grunnbeløpGjelderTil,
                "riktig_seks_g" to riktigSeksG
            )).map { AnvendtGrunnbeløpDto(
                aktørId = it.string("aktor_id"),
                personidentifikator = it.string("personidentifikator"),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                `6G` = it.double("seks_g"),
            ) }.asList)
        }
    }
}