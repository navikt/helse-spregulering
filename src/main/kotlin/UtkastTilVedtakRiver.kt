import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UtkastTilVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao
): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "utkast_til_vedtak") }
            validate {
                it.requireKey("sykepengegrunnlagsfakta.6G", "fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt ->
                    val dato = LocalDate.parse(skjæringstidspunkt.asText())
                    check(dato >= Virkningsdato2020Grunnbeløp)
                }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Lagrer nytting data til potensiell G-regulering:\n\t${packet.toJson()}")
        val anvendtGrunnbeløpDto = AnvendtGrunnbeløpDto(
            personidentifikator = packet["fødselsnummer"].asText(),
            skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
            `6G` = SeksG(packet["sykepengegrunnlagsfakta.6G"].asDouble())
        )
        anvendtGrunnbeløpDao.lagre(anvendtGrunnbeløpDto)
    }

    private companion object {
        /**
         * Perioder med skjæringstidspunkt før denne datoen ble G-regulert på en annen måte.
         * Dette var før revurderingenes tid i Spleis, og dette ble gjort med egne utbetalinger
         * av typen ETTERUTBETALING. De kan ikke identifiseres ved hjelp av mekanismen Spregulering belager seg på.
         */
        private val Virkningsdato2020Grunnbeløp = LocalDate.parse("2020-09-21")
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}