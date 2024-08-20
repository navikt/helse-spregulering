import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KjørGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kjør_grunnbeløpsregulering")
                it.require("grunnbeløpGjelderFra") { grunnbeløpGjelderFra -> LocalDate.parse(grunnbeløpGjelderFra.asText()) }
                it.require("riktigGrunnbeløp") { riktigGrunnbeløp -> SeksG.fraGrunnbeløp(riktigGrunnbeløp.asDouble()) }
                it.interestedIn("grunnbeløpGjelderTil") { grunnbeløpGjelderTil -> LocalDate.parse(grunnbeløpGjelderTil.asText()) }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val grunnbeløpGjelderFra = packet["grunnbeløpGjelderFra"].asLocalDate()
        val grunnbeløpGjelderTil = packet["grunnbeløpGjelderTil"].takeUnless { it.isMissingOrNull() }?.asLocalDate() ?: LocalDate.MAX
        val rikitgSeksG = SeksG.fraGrunnbeløp(packet["riktigGrunnbeløp"].asDouble())
        val feilanvendteGrunnbeløp = anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(grunnbeløpGjelderFra, grunnbeløpGjelderTil, rikitgSeksG)
        sikkerlogg.info("Grunnbeløpsregulerer ${feilanvendteGrunnbeløp.size} sykefraværstilfeller:\n\t${packet.toJson()}")
        feilanvendteGrunnbeløp.forEach {
            val grunnbeløpsreguleringEvent = it.toGrunnbeløpsreguleringEvent()
            sikkerlogg.info("Sender grunnbeløpsregulering:\n\t${grunnbeløpsreguleringEvent}")
            context.publish(it.personidentifikator, grunnbeløpsreguleringEvent)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun AnvendtGrunnbeløpDto.toGrunnbeløpsreguleringEvent() = JsonMessage.newMessage("grunnbeløpsregulering", mapOf(
            "fødselsnummer" to personidentifikator,
            "aktørId" to aktørId,
            "skjæringstidspunkt" to skjæringstidspunkt
        )).toJson()
    }
}