import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SykefraværstilfelleIkkeFunnetRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "sykefraværstilfelle_ikke_funnet")
                it.requireKey("fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt -> LocalDate.parse(skjæringstidspunkt.asText()) }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Sletter sykefraværstilfelle:\n\t${packet.toJson()}")
        anvendtGrunnbeløpDao.slettSykefraværstilfelle(packet["fødselsnummer"].asText(), packet["skjæringstidspunkt"].asLocalDate())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}