import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv()
    val dataSourceBuilder = DataSourceBuilder(env)
    val anvendtGrunnbeløpDao = AnvendtGrunnbeløpDao(dataSourceBuilder.getDataSource())
    RapidApplication.create(env).apply {
        UtkastTilVedtakRiver(this, anvendtGrunnbeløpDao)
        ManuellGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
        SykefraværstilfelleIkkeFunnetRiver(this, anvendtGrunnbeløpDao)
        AutomatiskGrunnbeløpsreguleringRiver(this, anvendtGrunnbeløpDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}