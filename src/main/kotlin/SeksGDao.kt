import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class SeksGDao(private val dataSource: DataSource) {

    fun registrer(seksG: Double, skjæringstidspunkt: LocalDate) {
        @Language("PostgreSQL")
        val statement = """
            insert into seks_g (seks_g, tidligste_skjaeringstidspunkt, seneste_skjaeringstidspunkt) values (:seks_g, :forste, :siste)
            on conflict(seks_g) do update set tidligste_skjaeringstidspunkt = least(seks_g.tidligste_skjaeringstidspunkt, EXCLUDED.tidligste_skjaeringstidspunkt), 
            seneste_skjaeringstidspunkt = greatest(seks_g.seneste_skjaeringstidspunkt, EXCLUDED.seneste_skjaeringstidspunkt)
        """.trimIndent()

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement, mapOf(
                        "seks_g" to seksG,
                        "forste" to skjæringstidspunkt,
                        "siste" to skjæringstidspunkt
                    )
                ).asExecute
            )
        }
    }

    fun finnObservasjon(seksG: Double): Observasjon? {
        @Language("PostgreSQL")
        val statement = """
            select seks_g, seks_g.tidligste_skjaeringstidspunkt, seks_g.seneste_skjaeringstidspunkt from seks_g where seks_g.seks_g = :seks_g
        """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement, mapOf(
                        "seks_g" to seksG,
                    )
                ).map { row ->
                    Observasjon(
                        row.double("seks_g"),
                        row.localDate("tidligste_skjaeringstidspunkt"),
                        row.localDate("seneste_skjaeringstidspunkt")
                    )
                }.asSingle
            )
        }
    }

    data class Observasjon(
        val seksG: Double,
        val tidligsteSkjaeringstidspunkt: LocalDate,
        val senesteSkjaeringstidspunkt: LocalDate
    )
}