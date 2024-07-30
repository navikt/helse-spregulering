import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
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
}