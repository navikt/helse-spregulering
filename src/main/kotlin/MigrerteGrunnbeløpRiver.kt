import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MigrerteGrunnbeløpRiver(
    rapidsConnection: RapidsConnection,
    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao,
    val seksGDato: SeksGDao
): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "grunnbeløp")
                it.requireKey("aktørId", "fødselsnummer")
                it.requireArray("grunnbeløp") {
                    require("skjæringstidspunkt") { skjæringstidspunkt -> LocalDate.parse(skjæringstidspunkt.asText()) }
                    requireKey("6G")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info("Migrerer inn grunnbeløp:\n\t${packet.toJson()}")
        val anvendtGrunnbeløp = packet["grunnbeløp"].map { AnvendtGrunnbeløpDto(
            aktørId = packet["aktørId"].asText(),
            personidentifikator = packet["fødselsnummer"].asText(),
            skjæringstidspunkt = it["skjæringstidspunkt"].asLocalDate(),
            `6G`= it["6G"].asDouble()
        )}

        anvendtGrunnbeløp.forEach {
            anvendtGrunnbeløpDao.lagre(it)
            seksGDato.registrer(it.`6G`, it.skjæringstidspunkt)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}