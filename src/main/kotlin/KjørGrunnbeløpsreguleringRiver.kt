import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KjørGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kjør_grunnbeløpsregulering")
                it.require("grunnbeløpGjelderFra") { grunnbeløpGjelderFra -> LocalDate.parse(grunnbeløpGjelderFra.asText()) }
                it.require("riktigGrunnbeløp") { riktigGrunnbeløp -> check(riktigGrunnbeløp.asDouble() > 100_000.0 && riktigGrunnbeløp.asDouble() < 600_000.0) }
                it.interestedIn("grunnbeløpGjelderTil") { grunnbeløpGjelderTil -> LocalDate.parse(grunnbeløpGjelderTil.asText()) }
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val grunnbeløpGjelderFra = packet["grunnbeløpGjelderFra"].asLocalDate()
        val grunnbeløpGjelderTil = packet["grunnbeløpGjelderTil"].takeUnless { it.isMissingOrNull() }?.asLocalDate() ?: LocalDate.MAX
        val rikitgGrunnbeløp = packet["riktigGrunnbeløp"].asDouble()
        val feilanvendteGrunnbeløp = anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(grunnbeløpGjelderFra, grunnbeløpGjelderTil, rikitgGrunnbeløp)
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