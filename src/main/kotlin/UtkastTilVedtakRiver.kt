import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UtkastTilVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao
): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "utkast_til_vedtak")
                it.requireKey("sykepengegrunnlagsfakta.6G", "aktørId", "fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt ->
                    val dato = LocalDate.parse(skjæringstidspunkt.asText())
                    check(dato >= Virkningsdato2020Grunnbeløp)
                }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Lagrer nytting data til potensiell G-regulering:\n\t${packet.toJson()}")
        val anvendtGrunnbeløpDto = AnvendtGrunnbeløpDto(
            aktørId = packet["aktørId"].asText(),
            personidentifikator = packet["fødselsnummer"].asText(),
            skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
            `6G` = packet["sykepengegrunnlagsfakta.6G"].asDouble()
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