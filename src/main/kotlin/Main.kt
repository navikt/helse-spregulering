import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    val env = System.getenv()
    val dataSourceBuilder = DataSourceBuilder(env)
    val anvendtGrunnbeløpDao = AnvendtGrunnbeløpDao(dataSourceBuilder.getDataSource())
    val seksGDao = SeksGDao(dataSourceBuilder.getDataSource())
    RapidApplication.create(env).apply {
        UtkastTilVedtakRiver(this, anvendtGrunnbeløpDao, seksGDao)
        KjørGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
        SykefraværstilfelleIkkeFunnetRiver(this, anvendtGrunnbeløpDao)
        MigrerteGrunnbeløpRiver(this, anvendtGrunnbeløpDao, seksGDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}