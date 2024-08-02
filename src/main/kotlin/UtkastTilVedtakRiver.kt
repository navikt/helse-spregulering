import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UtkastTilVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao,
    val seksGDato: SeksGDao
): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "utkast_til_vedtak")
                it.requireKey("sykepengegrunnlagsfakta.6G", "aktørId", "fødselsnummer")
                it.require("skjæringstidspunkt") { skjæringstidspunkt -> LocalDate.parse(skjæringstidspunkt.asText()) }
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
        seksGDato.registrer(anvendtGrunnbeløpDto.`6G`, anvendtGrunnbeløpDto.skjæringstidspunkt)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}