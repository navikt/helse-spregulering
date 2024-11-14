import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SykefraværstilfelleIkkeFunnetRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "sykefraværstilfelle_ikke_funnet") }
            validate {
                it.requireKey("fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt -> LocalDate.parse(skjæringstidspunkt.asText()) }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Sletter sykefraværstilfelle:\n\t${packet.toJson()}")
        anvendtGrunnbeløpDao.slettSykefraværstilfelle(packet["fødselsnummer"].asText(), packet["skjæringstidspunkt"].asLocalDate())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}