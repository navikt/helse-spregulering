import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UtkastTilVedtakRiver(rapidsConnection: RapidsConnection): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("avsluttet_med_vedtak", "utkast_til_vedtak"))
                it.requireKey("sykepengegrunnlagsfakta.6G", "aktørId", "fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt -> LocalDate.parse(skjæringstidspunkt.asText()) }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Lagrer snart nytting data til potensiell G-regulering:\n\t${packet.toJson()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}